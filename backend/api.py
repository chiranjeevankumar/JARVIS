"""
AURA Backend API
================

Minimal server foundation for the AURA account + AI architecture.

IMPORTANT:
- No Gemini API key is stored here.
- No Gemini API call is made here yet.
- Android is not modified by this module.
"""

from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel


app = FastAPI(
    title="AURA Backend API",
    version="0.9.0",
    description="Backend foundation for AURA.",
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
        service="AURA Backend",
        version="0.9.0",
        timestamp=datetime.now(
            timezone.utc
        ).isoformat(),
    )


@app.post("/api/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    """
    Temporary chat endpoint.

    Gemini integration will be added in a later controlled
    checkpoint. This endpoint intentionally does not contain
    or request a Gemini API key from the client.
    """

    message = request.message.strip()

    if not message:
        return ChatResponse(
            success=False,
            message="Message cannot be empty.",
            user_id=request.user_id,
        )

    return ChatResponse(
        success=True,
        message=(
            "AURA backend received your message. "
            "AI provider integration is not enabled yet."
        ),
        user_id=request.user_id,
    )
