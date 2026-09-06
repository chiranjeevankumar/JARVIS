"""
AURA AI Provider Factory

Centralizes AI provider construction so the API layer depends
on the provider factory rather than a concrete provider.
"""

from brain.ai_provider import AIProvider
from brain.gemini_provider import GeminiProvider


def create_ai_provider() -> AIProvider:
    """
    Create AURA's configured internal AI provider.

    Concrete provider selection remains inside the brain layer.
    """
    return GeminiProvider()
