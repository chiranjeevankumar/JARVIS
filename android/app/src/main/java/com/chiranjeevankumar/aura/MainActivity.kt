package com.chiranjeevankumar.aura

import android.app.Activity
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
        "https://chiruagent.app.n8n.cloud/webhook-test/aura-bridge-test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = "AURA\n\nAndroid Bridge v0.1.3\n\nONLINE"
            textSize = 26f
            setPadding(40, 80, 40, 40)
        }

        setContentView(text)

        sendTestCommand()
    }

    private fun sendTestCommand() {

        thread {

            try {

                val json = JSONObject()
                json.put("message", "Open YouTube")

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                val result = response.body?.string() ?: ""

                runOnUiThread {
                    updateStatus(result)
                }

            } catch (e: Exception) {

                runOnUiThread {
                    updateStatus("ERROR\n${e.message}")
                }
            }
        }
    }

    private fun updateStatus(result: String) {

        val view = TextView(this).apply {
            text = "AURA\n\nn8n Bridge Test\n\n$result"
            textSize = 20f
            setPadding(40, 80, 40, 40)
        }

        setContentView(view)
    }
}
