package com.chiranjeevankumar.aura

import android.util.Log

import android.net.Uri
import java.net.URLEncoder

import android.app.Activity
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private var ttsReady = false

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
        private const val REQUEST_SPEECH = 1002
    }
// AURA v0.7 PART 1B-B
    // Native deterministic command engine.
    private val commandEngine = AuraCommandEngine()
private lateinit var statusText: TextView
    private lateinit var commandInput: EditText
    private lateinit var voiceInputButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()

        textToSpeech = TextToSpeech(
            this,
            this
        )
    }

override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_RECORD_AUDIO) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

            } else {

                statusText.text =
                    "AURA\n\nMicrophone permission denied."
            }
        }
    }

    @Deprecated("Deprecated in Android API  Activity result handling")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == REQUEST_SPEECH &&
            resultCode == RESULT_OK
        ) {

            val results =
                data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

            val command =
                results?.firstOrNull()?.trim().orEmpty()

            if (command.isNotEmpty()) {

                commandInput.setText(command)

                statusText.text =
                    "AURA\n\nHeard:\n$command\n\nSending..."

                sendCommand(command)

            } else {

                statusText.text =
                    "AURA\n\nI didn't hear a command."
            }
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            val result = textToSpeech.setLanguage(
                java.util.Locale.getDefault()
            )

            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED

            if (!ttsReady) {
                statusText.text =
                    "AURA\\n\\nText-to-Speech language unavailable."
            }

        } else {

            ttsReady = false

            statusText.text =
                "AURA\\n\\nText-to-Speech initialization failed."
        }
    }


    private fun createInterface() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 70, 40, 40)
        }

        val title = TextView(this).apply {
            text = "AURA"
            textSize = 32f
        }

        val subtitle = TextView(this).apply {
            text = "Personal AI Agent"
            textSize = 18f
        }

        commandInput = EditText(this).apply {
            hint = "Type your command..."
            textSize = 18f
            setSingleLine(true)
        }

        voiceInputButton = Button(this).apply {
            text = "🎙️ Voice Input"
            textSize = 18f
        }

        voiceInputButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    java.util.Locale.getDefault()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your command"
                )
            }

            try {
                statusText.text = "AURA\n\nListening..."
                startActivityForResult(intent, REQUEST_SPEECH)
            } catch (e: Exception) {
                statusText.text =
                    "AURA\n\nSpeech recognition is not available."
            }
        }

        val sendButton = Button(this).apply {
            text = "SEND"
            textSize = 18f
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 18f
            setPadding(0, 40, 0, 0)
        }

        sendButton.setOnClickListener {

            val command = commandInput.text.toString().trim()

            if (command.isEmpty()) {
                statusText.text = "Please enter a command."
                return@setOnClickListener
            }

            statusText.text = "AURA\n\nSending command..."

            sendCommand(command)
        }

        layout.addView(title)
        layout.addView(subtitle)

        layout.addView(
            commandInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(sendButton)
        layout.addView(voiceInputButton)


        layout.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
    }

    private fun speak(text: String) {

        if (!ttsReady) {
            return
        }

        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "AURA_TTS"
        )
    }

    private fun sendCommand(command: String) {

        Log.d("AURA", "LOCAL COMMAND=$command")

        val result = commandEngine.processNaturalLanguage(command)

        when (result.action) {

            AuraAction.OPEN_APP -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                openApp(result.target)
            }

            AuraAction.SEARCH_WEB -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                searchWeb(result.target)
            }

            AuraAction.OPEN_QUICK_SETTINGS -> {
                statusText.text =
                    "AURA\n\n${result.message}"

                try {
                    startActivity(
                        Intent("android.settings.panel.action.INTERNET_CONNECTIVITY")
                    )
                } catch (e: Exception) {
                    Log.e("AURA", "Quick Settings panel failed", e)

                    try {
                        startActivity(
                            Intent(android.provider.Settings.ACTION_SETTINGS)
                        )
                    } catch (fallback: Exception) {
                        Log.e("AURA", "Settings fallback failed", fallback)
                    }
                }
            }

            AuraAction.OPEN_NOTIFICATIONS -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                try {
                    val intent = Intent("android.settings.NOTIFICATION_SETTINGS")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AURA", "Failed to open notification settings", e)
                    statusText.text =
                        "AURA\n\nUnable to open notifications."
                }
            }

            AuraAction.OPEN_SETTINGS -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_SETTINGS
                    )

                    startActivity(intent)

                } catch (e: Exception) {

                    statusText.text =
                        "AURA\n\nCould not open Settings.\n\n" +
                        e.javaClass.simpleName
                }
            }

            AuraAction.GO_HOME -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                val homeIntent = Intent(
                    Intent.ACTION_MAIN
                ).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }

                startActivity(homeIntent)
            }

            AuraAction.TIME -> {
                val currentTime = java.text.SimpleDateFormat(
                    "h:mm a",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                android.widget.Toast.makeText(
                    this,
                    "The time is $currentTime",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            AuraAction.ANSWER -> {

                statusText.text =
                    "AURA\n\n${result.message}"

                speak(result.message)
            }

            else -> {

                statusText.text =
                    "AURA\n\n${result.message}"
            }
        }
    }

    private fun searchWeb(query: String) {

        try {

            val encodedQuery =
                URLEncoder.encode(
                    query,
                    "UTF-8"
                )

            val url =
                "https://www.google.com/search?q=$encodedQuery"

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            startActivity(intent)

        } catch (e: Exception) {

            statusText.text =
                "AURA\\n\\nUnable to open search:\\n${e.message}"
        }
    }


    private fun openApp(appName: String) {

        val requested = appName
            .trim()
            .lowercase()

        try {

            val launcherIntent = Intent(
                Intent.ACTION_MAIN,
                null
            ).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val apps = packageManager
                .queryIntentActivities(
                    launcherIntent,
                    0
                )

            val match = apps.firstOrNull { info ->

                val label = info
                    .loadLabel(packageManager)
                    .toString()
                    .trim()
                    .lowercase()

                label == requested ||
                    label.contains(requested) ||
                    requested.contains(label)
            }

            if (match == null) {

                runOnUiThread {
                    statusText.text =
                        "AURA\n\nApp not found:\n$appName"
                }

                return
            }

            val launchIntent = Intent(
                Intent.ACTION_MAIN
            ).apply {

                addCategory(
                    Intent.CATEGORY_LAUNCHER
                )

                component = android.content.ComponentName(
                    match.activityInfo.packageName,
                    match.activityInfo.name
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

            startActivity(launchIntent)

            runOnUiThread {
                statusText.text =
                    "AURA\n\nOpened:\n$appName"

                speak("Opening $appName")
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text =
                    "AURA\n\nCould not open:\n$appName\n\n" +
                    e.javaClass.simpleName
            }
        }
    }

    override fun onDestroy() {

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }

}
