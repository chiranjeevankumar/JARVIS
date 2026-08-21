package com.chiranjeevankumar.aura

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val client = OkHttpClient()

    private val webhookUrl =
        "https://chiruagent.app.n8n.cloud/webhook/aura-bridge-test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showStatus("AURA\n\nAndroid Bridge v0.1.4\n\nONLINE")

        sendCommand("Open YouTube")
    }

    private fun sendCommand(message: String) {

        thread {

            try {

                val json = JSONObject()
                json.put("message", message)

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                val result = response.body?.string() ?: ""

                runOnUiThread {
                    handleCommand(result)
                }

            } catch (e: Exception) {

                runOnUiThread {
                    showStatus(
                        "AURA\n\nERROR\n\n${e.message}"
                    )
                }
            }
        }
    }

    private fun handleCommand(result: String) {

        try {

            val command = JSONObject(result)

            val action = command.optString("action")
            val app = command.optString("app")

            when (action) {

                "OPEN_APP" -> {

                    if (app.equals("YouTube", ignoreCase = true)) {

                        showStatus(
                            "AURA\n\nOpening YouTube..."
                        )

                        openYouTube()

                    } else {

                        showStatus(
                            "AURA\n\nUnknown app:\n$app"
                        )
                    }
                }

                "NONE" -> {

                    showStatus(
                        "AURA\n\n${command.optString("message")}"
                    )
                }

                else -> {

                    showStatus(
                        "AURA\n\nUnknown command:\n$result"
                    )
                }
            }

        } catch (e: Exception) {

            showStatus(
                "AURA\n\nInvalid command:\n$result"
            )
        }
    }

    private fun openYouTube() {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.youtube.com/")
            )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Let Android choose the appropriate installed application.
            startActivity(intent)

        } catch (e: android.content.ActivityNotFoundException) {

            showStatus(
                "AURA\\n\\nNo application is available to open YouTube."
            )

        } catch (e: Exception) {

            showStatus(
                "AURA\\n\\nYouTube launch error:\\n${e.message}"
            )
        }
    }

    private fun showStatus(message: String) {

        val view = TextView(this).apply {

            text = message
            textSize = 24f
            setPadding(40, 80, 40, 40)
        }

        setContentView(view)
    }
}
