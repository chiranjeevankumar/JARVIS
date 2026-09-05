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
from google import genai


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

    client = get_gemini_client()

    if client is None:
        return ChatResponse(
            success=False,
            message="AURA AI provider is not configured.",
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
            message=f"AURA AI provider request failed: {type(e).__name__}: {e}",
            user_id=request.user_id,
        )
