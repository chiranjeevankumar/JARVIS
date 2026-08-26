package com.chiranjeevankumar.aura

/**
 * AURA v0.7
 *
 * Native deterministic command interpreter.
 *
 * This class ONLY interprets commands.
 * Android actions are executed by the caller.
 */
class AuraCommandEngine {

    fun process(command: String): AuraActionResult {

        val cleanCommand = command
            .trim()
            .lowercase()

        if (cleanCommand.isEmpty()) {
            return AuraActionResult(
                action = AuraAction.ANSWER,
                message = "Please enter a command."
            )
        }

        // ----------------------------------------------------
        // OPEN APP
        // Examples:
        // open YouTube
        // open WhatsApp
        // open Chrome
        // ----------------------------------------------------

        val openPrefix = "open "

        if (cleanCommand.startsWith(openPrefix)) {

            val appName = command
                .trim()
                .substring(openPrefix.length)
                .trim()

            if (appName.isNotEmpty()) {

                return AuraActionResult(
                    action = AuraAction.OPEN_APP,
                    target = appName,
                    message = "Opening $appName"
                )
            }
        }

        // ----------------------------------------------------
        // SEARCH WEB
        // Examples:
        // search cats
        // search for cats
        // ----------------------------------------------------

        val searchForPrefix = "search for "
        val searchPrefix = "search "

        if (cleanCommand.startsWith(searchForPrefix)) {

            val query = command
                .trim()
                .substring(searchForPrefix.length)
                .trim()

            if (query.isNotEmpty()) {

                return AuraActionResult(
                    action = AuraAction.SEARCH_WEB,
                    target = query,
                    message = "Searching for $query"
                )
            }
        }

        if (cleanCommand.startsWith(searchPrefix)) {

            val query = command
                .trim()
                .substring(searchPrefix.length)
                .trim()

            if (query.isNotEmpty()) {

                return AuraActionResult(
                    action = AuraAction.SEARCH_WEB,
                    target = query,
                    message = "Searching for $query"
                )
            }
        }

        // ----------------------------------------------------
        // UNKNOWN
        // ----------------------------------------------------

        return AuraActionResult(
            action = AuraAction.ANSWER,
            message = "I don't understand that command yet."
        )
    }
}

/**
 * Structured result returned by AuraCommandEngine.
 */
data class AuraActionResult(
    val action: String,
    val target: String = "",
    val message: String = ""
)

/**
 * Native AURA actions.
 */
object AuraAction {

    const val OPEN_APP = "OPEN_APP"
    const val SEARCH_WEB = "SEARCH_WEB"
    const val ANSWER = "ANSWER"
}
