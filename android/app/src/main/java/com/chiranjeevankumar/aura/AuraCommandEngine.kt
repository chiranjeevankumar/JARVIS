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
        // GO HOME
        //
        // Supported examples:
        // go home
        // home
        // take me home
        // go to home screen
        // ----------------------------------------------------

        val homeCommands = setOf(
            "go home",
            "home",
            "take me home",
            "go to home screen"
        )

        if (cleanCommand in homeCommands) {
            return AuraActionResult(
                action = AuraAction.GO_HOME,
                message = "Going home."
            )
        }

        // ----------------------------------------------------
        // OPEN SETTINGS
        //
        // Supported examples:
        // open settings
        // settings
        // go to settings
        // open android settings
        // ----------------------------------------------------

        val settingsCommands = setOf(
            "open settings",
            "settings",
            "go to settings",
            "open android settings"
        )

        if (cleanCommand in settingsCommands) {
            return AuraActionResult(
                action = AuraAction.OPEN_SETTINGS,
                message = "Opening Settings."
            )
        }

        // ----------------------------------------------------

        // ----------------------------------------------------
        // OPEN NOTIFICATIONS
        // ----------------------------------------------------
        //
        // IMPORTANT:
        // This must be checked BEFORE OPEN APP because
        // commands such as "open notifications" begin with
        // "open " and would otherwise be interpreted as
        // an application name.
        //
        // Supported examples:
        // open notifications
        // notifications
        // open notification panel
        // show notifications
        // show notification panel
        // go to notifications
        // ----------------------------------------------------

        // ----------------------------------------------------
        // OPEN QUICK SETTINGS
        // ----------------------------------------------------
        //
        // Supported examples:
        // open quick settings
        // quick settings
        // open quick panel
        // show quick settings
        // show quick panel
        // go to quick settings
        // ----------------------------------------------------

        val quickSettingsCommands = listOf(
            "open quick settings",
            "quick settings",
            "open quick panel",
            "show quick settings",
            "show quick panel",
            "go to quick settings"
        )

        if (cleanCommand in quickSettingsCommands) {
            return AuraActionResult(
                action = AuraAction.OPEN_QUICK_SETTINGS,
                message = "Opening quick settings."
            )
        }

        val notificationCommands = listOf(
            "open notifications",
            "notifications",
            "open notification panel",
            "show notifications",
            "show notification panel",
            "go to notifications"
        )

        if (cleanCommand in notificationCommands) {
            return AuraActionResult(
                action = AuraAction.OPEN_NOTIFICATIONS,
                message = "Opening notifications."
            )
        }

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
        // TIME / DATE
        // ----------------------------------------------------

        val timeCommands = listOf(
            "what time is it",
            "tell me the time",
            "what's the time",
            "what is the time",
            "current time"
        )

        val dateCommands = listOf(
            "what is the date",
            "what's the date",
            "tell me the date",
            "what date is it",
            "today's date",
            "todays date"
        )

        if (cleanCommand in timeCommands) {
            val time = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            return AuraActionResult(
                action = AuraAction.ANSWER,
                message = "The time is $time."
            )
        }

        if (cleanCommand in dateCommands) {
            val date = java.text.SimpleDateFormat(
                "EEEE, d MMMM yyyy",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            return AuraActionResult(
                action = AuraAction.ANSWER,
                message = "Today is $date."
            )
        }

        // ----------------------------------------------------
        // OPEN NOTIFICATIONS
        // ----------------------------------------------------
        //
        // Supported examples:
        // open notifications
        // notifications
        // open notification panel
        // show notifications
        // show notification panel
        // go to notifications
        // ----------------------------------------------------



        // ----------------------------------------------------
        // UNKNOWN
        // ----------------------------------------------------

        return AuraActionResult(
            action = AuraAction.ANSWER,
            message = "I don't understand that command yet."
        )
    }
    
    /**
     * Phase 2 natural-language normalization.
     *
     * Converts common conversational instructions into the
     * deterministic commands already understood by process().
     *
     * This method does NOT execute Android actions.
     */
    private fun normalizeCommand(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace(Regex("[.!?,]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Removes conversational prefixes and wake-word phrases.
     */
    private fun removePolitePrefix(input: String): String {
        var command = input.trim()

        val prefixes = listOf(
            "hey jarvis ",
            "hey aura ",
            "jarvis ",
            "aura ",
            "please ",
            "can you ",
            "could you ",
            "would you ",
            "will you ",
            "kindly "
        )

        var changed = true

        while (changed) {
            changed = false

            for (prefix in prefixes) {
                if (command.startsWith(prefix)) {
                    command = command.removePrefix(prefix).trim()
                    changed = true
                    break
                }
            }
        }

        return command
    }

    /**
     * Understands common natural-language commands and converts
     * them into the deterministic command vocabulary used by process().
     */
    fun understandNaturalLanguage(input: String): String {
        var command = normalizeCommand(input)
        command = removePolitePrefix(command)

        if (command.isEmpty()) {
            return command
        }

        // --------------------------------------------------------
        // HOME
        // --------------------------------------------------------

        val homeCommands = setOf(
            "home",
            "go home",
            "go back home",
            "take me home",
            "take me back home",
            "go to home",
            "go to the home",
            "go to home screen",
            "go to the home screen",
            "take me to the home screen",
            "return home",
            "bring me home"
        )

        if (command in homeCommands) {
            return "go home"
        }

        if (
            command.contains("go back to the home screen") ||
            command.contains("go back home screen") ||
            command.contains("return to the home screen") ||
            command.contains("take me back to the home screen")
        ) {
            return "go home"
        }

        // --------------------------------------------------------
        // SETTINGS
        // --------------------------------------------------------

        val settingsCommands = setOf(
            "settings",
            "open settings",
            "go to settings",
            "go to the settings",
            "open the settings",
            "open android settings",
            "go to android settings",
            "take me to settings",
            "take me to the settings",
            "take me to android settings",
            "open device settings",
            "go to device settings",
            "open system settings",
            "go to system settings"
        )

        if (command in settingsCommands) {
            return "open settings"
        }

        if (
            command.contains("take me to settings") ||
            command.contains("take me to the settings") ||
            command.contains("go to the settings") ||
            command.contains("open the settings") ||
            command.contains("open android settings") ||
            command.contains("go to android settings") ||
            command.contains("device settings") ||
            command.contains("system settings")
        ) {
            return "open settings"
        }

        // --------------------------------------------------------
        // NOTIFICATIONS
        // --------------------------------------------------------

        val notificationCommands = setOf(
            "notifications",
            "open notifications",
            "show notifications",
            "open notification panel",
            "show notification panel",
            "go to notifications",
            "open the notification panel",
            "show the notification panel"
        )

        if (command in notificationCommands) {
            return "open notifications"
        }

        // --------------------------------------------------------
        // QUICK SETTINGS
        // --------------------------------------------------------

        val quickSettingsCommands = setOf(
            "quick settings",
            "open quick settings",
            "show quick settings",
            "go to quick settings",
            "open the quick settings",
            "show the quick settings"
        )

        if (command in quickSettingsCommands) {
            return "open quick settings"
        }

        // --------------------------------------------------------
        // OPEN APP
        // --------------------------------------------------------

        val appPrefixes = listOf(
            "open ",
            "launch ",
            "start ",
            "run ",
            "open the app ",
            "launch the app ",
            "start the app ",
            "open the ",
            "launch the ",
            "start the "
        )

        for (prefix in appPrefixes) {
            if (command.startsWith(prefix)) {
                val app = command
                    .removePrefix(prefix)
                    .trim()

                if (app.isNotEmpty()) {
                    return "open $app"
                }
            }
        }

        // Natural-language app-reference aliases.
        //
        // These aliases make spoken references such as
        // "the YouTube app" resolve to the same deterministic
        // target already understood by the command engine.
        val appReferenceAliases = mapOf(
            "the youtube app" to "youtube",
            "youtube app" to "youtube",
            "the instagram app" to "instagram",
            "instagram app" to "instagram",
            "the whatsapp app" to "whatsapp",
            "whatsapp app" to "whatsapp",
            "the chrome app" to "chrome",
            "chrome app" to "chrome",
            "the gmail app" to "gmail",
            "gmail app" to "gmail"
        )

        val appPhrases = listOf(
            "take me to ",
            "take me into ",
            "go to ",
            "go into ",
            "bring up ",
            "bring me to ",
            "show me ",
            "get me to ",
            "get me into ",
            "switch to ",
            "switch over to ",
            "open up "
        )

        for (prefix in appPhrases) {
            if (command.startsWith(prefix)) {
                var target = command
                    .removePrefix(prefix)
                    .trim()

                target = appReferenceAliases[target] ?: target

                if (
                    target.isNotEmpty() &&
                    target != "home" &&
                    target != "the home screen" &&
                    target != "settings" &&
                    target != "the settings"
                ) {
                    return "open $target"
                }
            }
        }

        // --------------------------------------------------------
        // SEARCH WEB
        // --------------------------------------------------------

        val searchPrefixes = listOf(
            "search the web for ",
            "search web for ",
            "search for ",
            "look up ",
            "look for ",
            "find online ",
            "search online for ",
            "search the internet for ",
            "search internet for ",
            "find on the web ",
            "find on the internet ",
            "find online for ",
            "google ",
            "google search ",
            "do a web search for ",
            "do an online search for ",
            "can you search for ",
            "can you look up ",
            "please search for ",
            "please look up "
        )

        for (prefix in searchPrefixes) {
            if (command.startsWith(prefix)) {
                val query = command
                    .removePrefix(prefix)
                    .trim()

                if (query.isNotEmpty()) {
                    return "search $query"
                }
            }
        }

        // --------------------------------------------------------
        // FALLBACK
        // --------------------------------------------------------

        return command
    }

    /**
     * Phase 2 public entry point.
     *
     * Natural language is normalized locally and then passed
     * through the existing deterministic command engine.
     */
    fun processNaturalLanguage(input: String): AuraActionResult {
        val understood = understandNaturalLanguage(input)
        return process(understood)
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
    const val GO_HOME = "GO_HOME"
    const val OPEN_SETTINGS = "OPEN_SETTINGS"
    const val OPEN_NOTIFICATIONS = "OPEN_NOTIFICATIONS"
    const val OPEN_QUICK_SETTINGS = "OPEN_QUICK_SETTINGS"
    const val TIME = "TIME"
    const val ANSWER = "ANSWER"
}
