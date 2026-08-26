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

        // ----------------------------------------------------
        // NATURAL OPEN APP COMMANDS
        //
        // Supported examples:
        // open YouTube
        // launch YouTube
        // start YouTube
        // go to YouTube
        // open the YouTube app
        // please open YouTube
        // ----------------------------------------------------

        val openPrefixes = listOf(
            "open ",
            "launch ",
            "start ",
            "go to ",
            "please open ",
            "please launch ",
            "please start "
        )

        var appName: String? = null

        for (prefix in openPrefixes) {
            if (cleanCommand.startsWith(prefix)) {
                appName = command
                    .trim()
                    .substring(prefix.length)
                    .trim()

                break
            }
        }

        if (!appName.isNullOrEmpty()) {

            var cleanedAppName = appName
                .trim()

            if (cleanedAppName.lowercase().endsWith(" app")) {
                cleanedAppName = cleanedAppName
                    .dropLast(4)
                    .trim()
            }

            if (cleanedAppName.isNotEmpty()) {

                return AuraActionResult(
                    action = AuraAction.OPEN_APP,
                    target = cleanedAppName,
                    message = "Opening $cleanedAppName"
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
