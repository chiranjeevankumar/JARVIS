"""
AURA Internal Gemini Provider

Adapter that keeps Gemini behind AURA's provider-independent
AIProvider interface.

Gemini is an internal implementation detail.
Clients must communicate with AURA, not directly with Gemini.
"""

import os
from typing import Optional

from google import genai

from brain.ai_provider import (
    AIProvider,
    AIUnderstandingResult,
)


class GeminiProvider(AIProvider):
    """
    AURA AIProvider implementation backed by Gemini.

    The Gemini credential is read only from the server
    environment. It is never supplied by the client.
    """

    name = "gemini-internal"

    def __init__(
        self,
        model: str = "gemini-3.5-flash-lite",
        api_key: Optional[str] = None,
    ):
        self.model = model

        self.api_key = (
            api_key
            if api_key is not None
            else os.environ.get("GEMINI_API_KEY", "").strip()
        )

        self._client = None

    def is_available(self) -> bool:
        return bool(self.api_key)

    def describe(self) -> str:
        return "AURA internal Gemini provider"

    def _get_client(self):
        if not self.is_available():
            return None

        if self._client is None:
            self._client = genai.Client(
                api_key=self.api_key
            )

        return self._client

    def understand(
        self,
        text: str
    ) -> AIUnderstandingResult:

        message = text.strip()

        if not message:
            return AIUnderstandingResult(
                success=False,
                intent="unknown",
                parameters={
                    "original_text": text
                },
                explanation="Message cannot be empty.",
                provider=self.name,
            )

        client = self._get_client()

        if client is None:
            return AIUnderstandingResult(
                success=False,
                intent="unknown",
                parameters={
                    "original_text": text
                },
                explanation=(
                    "Gemini internal provider is not configured."
                ),
                provider=self.name,
            )

        try:
            response = client.models.generate_content(
                model=self.model,
                contents=message,
            )

            response_text = (
                getattr(response, "text", None)
                or ""
            ).strip()

            if not response_text:
                return AIUnderstandingResult(
                    success=False,
                    intent="unknown",
                    parameters={
                        "original_text": text
                    },
                    explanation=(
                        "Internal AI provider returned "
                        "an empty response."
                    ),
                    raw_response=response,
                    provider=self.name,
                )

            return AIUnderstandingResult(
                success=True,
                intent="conversation",
                parameters={
                    "original_text": text
                },
                confidence=1.0,
                explanation=response_text,
                raw_response=response,
                provider=self.name,
            )

        except Exception as error:
            return AIUnderstandingResult(
                success=False,
                intent="unknown",
                parameters={
                    "original_text": text
                },
                explanation=(
                    "Internal AI provider request failed: "
                    f"{type(error).__name__}: {error}"
                ),
                provider=self.name,
            )
