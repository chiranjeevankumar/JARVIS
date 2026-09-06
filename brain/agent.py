
"""
AURA Personal Agent

Central orchestration layer.

Now includes Memory Manager.
"""

from brain.agent_models import (
    UserRequest,
    AgentResponse,
    ActionStatus
)

from brain.intent_router import IntentRouter
from brain.planner import AgentPlanner
from brain.safety import SafetyEngine
from brain.executor import AgentExecutor

from memory.manager import MemoryManager

from tools.registry import ToolRegistry
from tools.bootstrap import register_builtin_tools

from memory.context_manager import ConversationContext
from brain.followup_integration import FollowUpIntegration
from brain.ai_provider import AIProvider, FallbackProvider


class AURAAgent:

    def __init__(
        self,
        registry: ToolRegistry,
        memory_manager: MemoryManager = None,
        safety_engine: SafetyEngine = None,
        ai_provider: AIProvider = None
    ):

        self.registry = registry

        self.ai_provider = (
            ai_provider
            if ai_provider is not None
            else FallbackProvider()
        )

        register_builtin_tools(
            self.registry
        )

        self.router = IntentRouter()

        self.planner = AgentPlanner()

        self.safety = (
            safety_engine
            if safety_engine is not None
            else SafetyEngine()
        )

        self.executor = AgentExecutor(
            registry=self.registry,
            safety_engine=self.safety
        )

        self.memory = (
            memory_manager
            if memory_manager is not None
            else MemoryManager()
        )

        # ----------------------------------------------------
        # Conversational follow-up context
        # ----------------------------------------------------

        self.context = ConversationContext(
            memory_manager=self.memory
        )

        self.followup = FollowUpIntegration(
            context=self.context,
            executor=self.executor,
        )

    # ========================================================
    # MAIN ENTRY POINT
    # ========================================================

    def run(
        self,
        text: str,
        confirmed: bool = False
    ) -> AgentResponse:

        # ----------------------------------------------------
        # Conversational follow-up path
        # ----------------------------------------------------

        followup = self.followup.handle(
            text,
            user_confirmed=confirmed,
        )

        if followup.handled:

            self.context.add_user(
                text
            )

            pipeline_result = followup.result

            if pipeline_result.success:

                message = pipeline_result.message

                self.context.add_assistant(
                    message,
                    data={
                        "type": "action_result",
                        "tool": pipeline_result.action.tool
                        if pipeline_result.action
                        else None,
                        "parameters": (
                            pipeline_result.action.parameters
                            if pipeline_result.action
                            else {}
                        ),
                    },
                )

                return AgentResponse(
                    message=message,
                    success=True,
                    plan=None,
                    results=[
                        pipeline_result.execution_result
                    ]
                )

            # Preserve confirmation/permission/block
            # information instead of bypassing safety.

            self.context.add_assistant(
                pipeline_result.message,
                data={
                    "type": "pipeline_result",
                    "stage": pipeline_result.stage,
                },
            )

            return AgentResponse(
                message=pipeline_result.message,
                success=False,
                plan=None,
                results=[]
            )

        request = UserRequest(
            text=text
        )

        # ----------------------------------------------------
        # Remember current request
        # ----------------------------------------------------

        self.memory.remember_recent(
            text,
            metadata={
                "type": "user_request"
            }
        )

        # ----------------------------------------------------
        # Understand
        # ----------------------------------------------------

        intent = self.router.route(
            request
        )

        # ----------------------------------------------------
        # Direct memory save
        #
        # Memory is an internal AURA capability. It is handled
        # directly by MemoryManager rather than being sent to
        # AgentExecutor as an external tool.
        # ----------------------------------------------------

        if intent.name == "remember":

            memory_text = intent.parameters.get(
                "memory",
                ""
            ).strip()

            if not memory_text:

                return AgentResponse(
                    message="I need something to remember.",
                    success=False,
                    plan=None,
                    results=[]
                )

            try:

                self.memory.remember(
                    memory_text,
                    memory_type="fact",
                    importance=0.8
                )

                self.memory.remember_recent(
                    f"Saved memory: {memory_text}",
                    metadata={
                        "type": "memory_saved"
                    }
                )

                return AgentResponse(
                    message=(
                        "I'll remember that: "
                        + memory_text
                    ),
                    success=True,
                    plan=None,
                    results=[]
                )

            except Exception as error:

                return AgentResponse(
                    message=(
                        "I couldn't save that memory: "
                        + str(error)
                    ),
                    success=False,
                    plan=None,
                    results=[]
                )

        # ----------------------------------------------------
        # Direct memory recall
        # ----------------------------------------------------

        if intent.name == "recall":

            query = intent.parameters.get(
                "query",
                ""
            )

            memories = self.memory.recall(
                query
            )

            if not memories:

                message = (
                    "I don't have any stored memory "
                    "matching that."
                )

            else:

                message = (
                    "I remember: "
                    + " | ".join(
                        item.content
                        for item in memories
                    )
                )

            return AgentResponse(
                message=message,
                success=True,
                plan=None,
                results=[]
            )

        # ----------------------------------------------------
        # Create plan
        # ----------------------------------------------------

        plan = self.planner.create_plan(
            request=request,
            intent=intent
        )

        # ----------------------------------------------------
        # Unknown
        # ----------------------------------------------------

        if intent.name == "unknown":

            # ------------------------------------------------
            # Internal AI provider fallback
            #
            # AURA remains the client-facing API.
            # The provider is an internal implementation detail.
            # ------------------------------------------------

            try:
                ai_result = self.ai_provider.understand(text)

                if ai_result.success:
                    return AgentResponse(
                        message=ai_result.explanation,
                        success=True,
                        plan=plan,
                        results=[]
                    )

                return AgentResponse(
                    message=ai_result.explanation,
                    success=False,
                    plan=plan,
                    results=[]
                )

            except Exception as error:
                return AgentResponse(
                    message=(
                        "AURA internal AI provider failed: "
                        f"{type(error).__name__}: {error}"
                    ),
                    success=False,
                    plan=plan,
                    results=[]
                )

        # ----------------------------------------------------
        # Execute
        # ----------------------------------------------------

        results = self.executor.execute_plan(
            plan,
            user_confirmed=confirmed
        )

        # ----------------------------------------------------
        # Handle no result
        # ----------------------------------------------------

        if not results:

            return AgentResponse(
                message="No action was executed.",
                success=False,
                plan=plan,
                results=[]
            )

        last_result = results[-1]

        # ----------------------------------------------------
        # Memory save result
        # ----------------------------------------------------

        if (
            intent.name == "remember"
            and last_result.success
        ):

            memory_text = intent.parameters.get(
                "memory",
                ""
            )

            self.memory.remember_recent(
                f"Saved memory: {memory_text}",
                metadata={
                    "type": "memory_saved"
                }
            )

            return AgentResponse(
                message=(
                    "I'll remember that: "
                    + memory_text
                ),
                success=True,
                plan=plan,
                results=results
            )

        # ----------------------------------------------------
        # Confirmation
        # ----------------------------------------------------

        if (
            last_result.status
            == ActionStatus.WAITING_CONFIRMATION
        ):

            return AgentResponse(
                message=(
                    "This action requires your "
                    "confirmation before I can continue."
                ),
                success=False,
                plan=plan,
                results=results
            )

        # ----------------------------------------------------
        # Success
        # ----------------------------------------------------

        if last_result.success:

            # ------------------------------------------------
            # Record successful normal action in conversation
            # context so future references can resolve it.
            # ------------------------------------------------

            if plan.actions:

                last_action = plan.actions[-1]

                self.context.add_user(
                    text,
                    data={
                        "type": "action_request",
                        "tool": last_action.tool,
                        "parameters": last_action.parameters,
                    },
                )

                self.context.add_assistant(
                    last_result.message,
                    data={
                        "type": "action_result",
                        "tool": last_action.tool,
                        "parameters": last_action.parameters,
                    },
                )

            return AgentResponse(
                message=last_result.message,
                success=True,
                plan=plan,
                results=results
            )

        # ----------------------------------------------------
        # Failure
        # ----------------------------------------------------

        return AgentResponse(
            message=last_result.message,
            success=False,
            plan=plan,
            results=results
        )
