"""
AURA Backend API
================

Backend foundation for the AURA account + AI architecture.

IMPORTANT:
- Gemini API key is read only from the server environment.
- No Gemini API key is accepted from the client.
- No Gemini API key is stored in source code.
- Android is not modified by this module.
"""

import os
from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel

from brain.agent import AURAAgent
from brain.provider_factory import create_ai_provider
from tools.registry import ToolRegistry


app = FastAPI(
    title="AURA API",
    version="1.0.0",
    description="The official AURA personal AI API.",
)


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    timestamp: str


class ChatRequest(BaseModel):
    message: str
    user_id: Optional[str] = None


class ChatResponse(BaseModel):
    success: bool
    message: str
    user_id: Optional[str] = None


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """Return backend health information."""

    return HealthResponse(
        status="ok",
        service="AURA API",
        version="1.0.0",
        timestamp=datetime.now(
            timezone.utc
        ).isoformat(),
    )


@app.get("/v1/status", response_model=HealthResponse)
def aura_status() -> HealthResponse:
    """Return official AURA API status."""

    return HealthResponse(
        status="ok",
        service="AURA API",
        version="1.0.0",
        timestamp=datetime.now(
            timezone.utc
        ).isoformat(),
    )


@app.post("/v1/chat", response_model=ChatResponse)
def aura_chat(
    request: ChatRequest,
    authorization: Optional[str] = Header(default=None),
) -> ChatResponse:

    """Send a message through AURA to the internal AI provider."""

    expected_key = os.environ.get("AURA_API_KEY", "").strip()

    if not expected_key:
        raise HTTPException(
            status_code=503,
            detail="AURA API authentication is not configured.",
        )

    supplied_key = ""

    if authorization and authorization.startswith("Bearer "):
        supplied_key = authorization[7:].strip()

    if supplied_key != expected_key:
        raise HTTPException(
            status_code=401,
            detail="Invalid AURA API key.",
        )

    message = request.message.strip()

    if not message:
        return ChatResponse(
            success=False,
            message="Message cannot be empty.",
            user_id=request.user_id,
        )

    # --------------------------------------------------------
    # AURA brain first
    #
    # Deterministic AURA capabilities such as memory and
    # commands must remain owned by AURA itself.
    # --------------------------------------------------------

    try:
        registry = ToolRegistry()
        ai_provider = create_ai_provider()
        agent = AURAAgent(
            registry=registry,
            ai_provider=ai_provider,
        )

        agent_response = agent.run(message)

        # AURA-native operations such as memory save/recall
        # intentionally return without an execution plan.
        # Planned commands have a non-unknown intent.
        aura_handled = (
            (
                agent_response.plan is None
                and agent_response.success
            )
            or (
                agent_response.plan is not None
                and agent_response.plan.intent.name != "unknown"
            )
        )

        if aura_handled:
            return ChatResponse(
                success=agent_response.success,
                message=agent_response.message,
                user_id=request.user_id,
            )

    except Exception as e:
        return ChatResponse(
            success=False,
            message=(
                "AURA brain request failed: "
                f"{type(e).__name__}: {e}"
            ),
            user_id=request.user_id,
        )

    # --------------------------------------------------------
    # AURAAgent already owns the internal AI-provider fallback.
    # If execution reaches this point, return the agent response
    # directly rather than invoking a second obsolete fallback.
    # --------------------------------------------------------

    return ChatResponse(
        success=agent_response.success,
        message=agent_response.message,
        user_id=request.user_id,
    )
