package com.chiranjeevankumar.aura

/**
 * AURA standalone AI provider abstraction.
 *
 * This interface contains no vendor-specific implementation,
 * credentials, or networking code.
 */
interface AuraAIProvider {

    /**
     * Sends a user message to the configured AI provider.
     */
    fun sendMessage(
        message: String,
        callback: (result: Result<String>) -> Unit
    )
}
