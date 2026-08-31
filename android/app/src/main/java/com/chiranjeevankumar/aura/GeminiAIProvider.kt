package com.chiranjeevankumar.aura

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * AURA AI provider.
 *
 * Android sends chat requests only to the AURA backend.
 *
 * IMPORTANT:
 * - No Gemini API key is stored here.
 * - No Gemini API key is accepted here.
 * - Android never calls Gemini directly.
 */
class GeminiAIProvider(
    private val backendUrl: String,
    private val userId: String? = null
) : AuraAIProvider {

    override fun sendMessage(
        message: String,
        callback: (result: Result<String>) -> Unit
    ) {
        thread {
            var connection: HttpURLConnection? = null

            try {
                if (message.isBlank()) {
                    throw IllegalArgumentException(
                        "Message is empty."
                    )
                }

                val startTime =
                    System.currentTimeMillis()

                val url = URL(
                    "${backendUrl.trimEnd('/')}/api/chat"
                )

                println(
                    "AURA_STEP48: START"
                )

                println(
                    "AURA_STEP48: URL=$url"
                )

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true

                println(
                    "AURA_STEP48: POST_CONFIGURED"
                )

                println(
                    "AURA_STEP48: CONNECT_TIMEOUT=15000"
                )

                println(
                    "AURA_STEP48: READ_TIMEOUT=30000"
                )

                val request = JSONObject()
                    .put("message", message)

                if (!userId.isNullOrBlank()) {
                    request.put("user_id", userId)
                }

                println(
                    "AURA_STEP48: SENDING_REQUEST"
                )

                connection.outputStream.use { output ->
                    output.write(
                        request.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                println(
                    "AURA_STEP48: REQUEST_SENT"
                )

                println(
                    "AURA_STEP48: WAITING_FOR_RESPONSE"
                )

                val responseCode =
                    connection.responseCode

                println(
                    "AURA_STEP48: HTTP_CODE=$responseCode"
                )

                println(
                    "AURA_STEP48: RESPONSE_TIME_MS=" +
                        (System.currentTimeMillis() - startTime)
                )

                val stream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText =
                    stream?.bufferedReader()?.use {
                        it.readText()
                    } ?: ""

                println(
                    "AURA_STEP48: RESPONSE_RECEIVED"
                )

                println(
                    "AURA_STEP48: RESPONSE_BODY=$responseText"
                )

                println(
                    "AURA_STEP48: TOTAL_TIME_MS=" +
                        (System.currentTimeMillis() - startTime)
                )

                if (responseCode !in 200..299) {
                    throw RuntimeException(
                        "AURA backend HTTP $responseCode: $responseText"
                    )
                }

                val response =
                    JSONObject(responseText)

                val success =
                    response.optBoolean("success", false)

                val responseMessage =
                    response.optString("message").trim()

                if (!success) {
                    throw RuntimeException(
                        if (responseMessage.isNotBlank()) {
                            responseMessage
                        } else {
                            "AURA backend request failed."
                        }
                    )
                }

                if (responseMessage.isBlank()) {
                    throw RuntimeException(
                        "AURA backend returned an empty response."
                    )
                }

                callback(
                    Result.success(responseMessage)
                )

            } catch (e: Exception) {

                println(
                    "AURA_STEP48: EXCEPTION_TYPE=" +
                        e.javaClass.name
                )

                println(
                    "AURA_STEP48: EXCEPTION_MESSAGE=" +
                        (e.message ?: "")
                )

                e.printStackTrace()

                callback(
                    Result.failure(e)
                )
            } finally {
                connection?.disconnect()
            }
        }
    }
}
