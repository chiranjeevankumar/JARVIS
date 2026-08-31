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

from fastapi import FastAPI
from pydantic import BaseModel
from google import genai


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


def get_gemini_client():
    """Create a Gemini client using the server-side environment key."""

    api_key = os.environ.get("GEMINI_API_KEY", "").strip()

    if not api_key:
        return None

    return genai.Client(api_key=api_key)


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
    """Send a user message to Gemini using the server-side API key."""

    message = request.message.strip()

    if not message:
        return ChatResponse(
            success=False,
            message="Message cannot be empty.",
            user_id=request.user_id,
        )

    client = get_gemini_client()

    if client is None:
        return ChatResponse(
            success=False,
            message="AI service is not configured.",
            user_id=request.user_id,
        )

    try:
        response = client.models.generate_content(
            model="gemini-3.5-flash-lite",
            contents=message,
        )

        text = (response.text or "").strip()

        if not text:
            return ChatResponse(
                success=False,
                message="AI service returned an empty response.",
                user_id=request.user_id,
            )

        return ChatResponse(
            success=True,
            message=text,
            user_id=request.user_id,
        )

    except Exception as e:
        return ChatResponse(
            success=False,
            message=f"AI service request failed: {type(e).__name__}: {e}",
            user_id=request.user_id,
        )
