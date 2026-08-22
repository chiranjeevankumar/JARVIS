package com.chiranjeevankumar.aura

import android.app.Activity
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
        private const val REQUEST_SPEECH = 1002
    }

    private val client = OkHttpClient()

    private val webhookUrl =
        "https://chiruagent.app.n8n.cloud/webhook/aura-v03"

    private lateinit var statusText: TextView
    private lateinit var commandInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()
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

        val sendButton = Button(this).apply {
            text = "SEND"
            textSize = 18f
        }

        val voiceButton = Button(this).apply {
            text = "🎤 VOICE"
            textSize = 18f
        }

        voiceButton.setOnClickListener {
            startVoiceInput()
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
        layout.addView(voiceButton)

        layout.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
    }

    private fun startVoiceInput() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )

            return
        }

        try {

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

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

            startActivityForResult(
                intent,
                REQUEST_SPEECH
            )

        } catch (e: Exception) {

            statusText.text =
                "AURA\n\nSpeech recognition unavailable."
        }
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

                startVoiceInput()

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

    private fun sendCommand(command: String) {

        thread {

            try {

                val json = JSONObject()
                json.put("command", command)

                val body = json.toString()
                    .toRequestBody(
                        "application/json".toMediaType()
                    )

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

                val response = client
                    .newCall(request)
                    .execute()

                val result =
                    response.body?.string() ?: ""

                runOnUiThread {

                    statusText.text =
                        "AURA\n\nn8n response:\n$result"

                    handleCommandResult(result)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text =
                        "AURA\n\nConnection error:\n${e.message}"
                }
            }
        }
    }

    private fun handleCommandResult(result: String) {

        try {

            val json = JSONObject(result)

            val action =
                json.optString("action")

            val app =
                json.optString("app")

            if (action == "OPEN_APP" && app.isNotEmpty()) {

                openApp(app)
            }

        } catch (_: Exception) {

            // Response wasn't JSON.
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
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text =
                    "AURA\n\nCould not open:\n$appName\n\n" +
                    e.javaClass.simpleName
            }
        }
    }
}
