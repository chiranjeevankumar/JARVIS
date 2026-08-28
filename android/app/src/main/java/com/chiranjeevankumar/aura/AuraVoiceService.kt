package com.chiranjeevankumar.aura

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AuraVoiceService : Service() {

    companion object {
        const val ACTION_START_WAKE =
            "com.chiranjeevankumar.aura.ACTION_START_WAKE"

        const val ACTION_STOP_WAKE =
            "com.chiranjeevankumar.aura.ACTION_STOP_WAKE"

        private const val CHANNEL_ID = "aura_voice_service"
        private const val TAG = "AURA_VOICE"
        private const val NOTIFICATION_ID = 7001

        private const val WAKE_PHRASE = "hey jarvis"
        private const val WAKE_PHRASE_ALT = "hey auras"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listeningForWake = false
    private var listeningForCommand = false

    private val handler = Handler(Looper.getMainLooper())
    private val commandEngine = AuraCommandEngine()

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "SERVICE onCreate")

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification("AURA is ready")
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            TAG,
            "SERVICE onStartCommand action=${intent?.action}"
        )

        when (intent?.action) {
            ACTION_START_WAKE -> startWakeListening()
            ACTION_STOP_WAKE -> {
                stopVoiceListening()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun createRecognizer(): Boolean {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable")
            return false
        }

        destroyRecognizer()

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            createRecognitionListener()
        )

        Log.d(TAG, "RECOGNIZER created")

        return true
    }

    private fun destroyRecognizer() {

        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null
    }

    private fun createRecognitionListener():
        RecognitionListener {

        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(
                    TAG,
                    "READY wake=$listeningForWake command=$listeningForCommand"
                )

                updateNotification(
                    if (listeningForWake)
                        "Listening for Hey JARVIS"
                    else
                        "Listening for command"
                )
            }

            override fun onBeginningOfSpeech() {
                Log.d(
                    TAG,
                    "BEGIN wake=$listeningForWake command=$listeningForCommand"
                )

                updateNotification(
                    if (listeningForWake)
                        "Hearing wake phrase..."
                    else
                        "Hearing command..."
                )
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "END OF SPEECH")
            }

            override fun onError(error: Int) {

                Log.e(
                    TAG,
                    "ERROR=$error wake=$listeningForWake command=$listeningForCommand"
                )

                if (listeningForWake) {
                    restartWakeListening()
                } else if (listeningForCommand) {

                    listeningForCommand = false
                    destroyRecognizer()

                    handler.postDelayed(
                        {
                            startWakeListening()
                        },
                        500L
                    )
                }
            }

            override fun onResults(results: Bundle?) {

                Log.d(TAG, "RESULTS received")

                val matches =
                    results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                Log.d(TAG, "MATCHES=$matches")

                val spokenText =
                    matches
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                Log.d(TAG, "FINAL TEXT=$spokenText")

                if (spokenText.isEmpty()) {

                    if (listeningForWake) {
                        restartWakeListening()
                    } else {
                        listeningForCommand = false
                        destroyRecognizer()

                        handler.postDelayed(
                            {
                                startWakeListening()
                            },
                            500L
                        )
                    }

                    return
                }

                processSpeechResult(spokenText)
            }

            override fun onPartialResults(
                partialResults: Bundle?
            ) {

                val partial =
                    partialResults
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                Log.d(TAG, "PARTIAL=$partial")
            }

            override fun onEvent(
                eventType: Int,
                params: Bundle?
            ) {
            }
        }
    }

    private fun startWakeListening() {

        if (!hasMicrophonePermission()) {
            updateNotification("Microphone permission required")
            return
        }

        listeningForWake = true
        listeningForCommand = false

        Log.d(TAG, "START WAKE")

        if (!createRecognizer()) {
            return
        }

        updateNotification(
            "Listening for Hey JARVIS"
        )

        startRecognizer(false)
    }

    private fun restartWakeListening() {

        if (!listeningForWake) {
            return
        }

        Log.d(TAG, "RESTART WAKE")

        destroyRecognizer()

        handler.postDelayed(
            {
                if (listeningForWake) {
                    startWakeListening()
                }
            },
            500L
        )
    }

    private fun processSpeechResult(text: String) {

        Log.d(TAG, "PROCESS=$text")

        val normalized =
            text
                .lowercase(Locale.getDefault())
                .trim()

        if (listeningForWake) {

            if (
                normalized.contains(WAKE_PHRASE) ||
                normalized.contains(WAKE_PHRASE_ALT) ||
                normalized.contains("hey jarvis")
            ) {

                Log.d(TAG, "HEY JARVIS DETECTED")

                listeningForWake = false
                listeningForCommand = true

                updateNotification(
                    "Hey JARVIS detected - speak command"
                )

                destroyRecognizer()

                handler.postDelayed(
                    {
                        if (listeningForCommand) {

                            Log.d(
                                TAG,
                                "COMMAND LISTENER START"
                            )

                            if (listeningForCommand) {
                                startCommandListening()
                            }
                        }
                    },
                    700L
                )

            } else {
                restartWakeListening()
            }

            return
        }

        if (listeningForCommand) {

            Log.d(
                TAG,
                "COMMAND RECEIVED=$text"
            )

            listeningForCommand = false

            destroyRecognizer()

            processCommand(text)

            // Command finished.
            // Do not restart the background wake listener.
            Log.d(
                TAG,
                "COMMAND COMPLETE — VOICE STOPPED"
            )

            stopVoiceListening()
            stopSelf()
        }
    }

    private fun startCommandListening() {

        if (!hasMicrophonePermission()) {
            updateNotification("Microphone permission required")
            return
        }

        Log.d(TAG, "START COMMAND")

        listeningForWake = false
        listeningForCommand = true

        if (!createRecognizer()) {
            return
        }

        updateNotification(
            "Listening for command"
        )

        startRecognizer(true)
    }

    private fun startRecognizer(commandMode: Boolean) {

        val recognizer = speechRecognizer

        if (recognizer == null) {
            Log.e(TAG, "Recognizer is null")
            return
        }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )

                putExtra(
                    RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    packageName
                )

                if (commandMode) {

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        10000L
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        8000L
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        1000L
                    )
                }
            }

        try {

            Log.d(
                TAG,
                "START LISTENING commandMode=$commandMode"
            )

            recognizer.startListening(intent)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "startListening failed",
                e
            )

            if (commandMode) {

                listeningForCommand = false
                destroyRecognizer()

                handler.postDelayed(
                    {
                        startWakeListening()
                    },
                    500L
                )

            } else {

                restartWakeListening()
            }
        }
    }

    private fun processCommand(command: String) {

        Log.d(
            TAG,
            "EXECUTING COMMAND=$command"
        )

        updateNotification(
            "Command: $command"
        )

        val result =
            commandEngine.processNaturalLanguage(command)

        Log.d(
            TAG,
            "ACTION=${result.action} TARGET=${result.target}"
        )

        when (result.action) {

            AuraAction.OPEN_APP -> {
                openApp(result.target)
            }

            AuraAction.SEARCH_WEB -> {
                searchWeb(result.target)
            }

            AuraAction.ANSWER -> {
                updateNotification(
                    "AURA heard: $command"
                )
            }
        }
    }

    private fun openApp(appName: String) {

        val requested =
            appName
                .trim()
                .lowercase(Locale.getDefault())

        try {

            val launcherIntent =
                Intent(
                    Intent.ACTION_MAIN,
                    null
                ).apply {
                    addCategory(
                        Intent.CATEGORY_LAUNCHER
                    )
                }

            val apps =
                packageManager.queryIntentActivities(
                    launcherIntent,
                    0
                )

            val match =
                apps.firstOrNull { info ->

                    val label =
                        info
                            .loadLabel(packageManager)
                            .toString()
                            .trim()
                            .lowercase(Locale.getDefault())

                    label == requested ||
                        label.contains(requested) ||
                        requested.contains(label)
                }

            if (match == null) {

                updateNotification(
                    "App not found: $appName"
                )

                return
            }

            val launchIntent =
                Intent(
                    Intent.ACTION_MAIN
                ).apply {

                    addCategory(
                        Intent.CATEGORY_LAUNCHER
                    )

                    component =
                        android.content.ComponentName(
                            match.activityInfo.packageName,
                            match.activityInfo.name
                        )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                }

            Log.d(
                TAG,
                "LAUNCHING ${match.activityInfo.packageName}"
            )

            startActivity(launchIntent)

            updateNotification(
                "Opened $appName"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "OPEN APP FAILED",
                e
            )

            updateNotification(
                "Could not open $appName"
            )
        }
    }

    private fun searchWeb(query: String) {

        try {

            val encoded =
                java.net.URLEncoder.encode(
                    query,
                    "UTF-8"
                )

            val url =
                "https://www.google.com/search?q=$encoded"

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(intent)

            updateNotification(
                "Searching for $query"
            )

        } catch (e: Exception) {

            updateNotification(
                "Unable to search"
            )
        }
    }

    private fun stopVoiceListening() {

        listeningForWake = false
        listeningForCommand = false

        handler.removeCallbacksAndMessages(null)

        destroyRecognizer()
    }

    private fun hasMicrophonePermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            true
        }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "AURA Voice",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        message: String
    ): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle("AURA")
                .setContentText(message)
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("AURA")
                .setContentText(message)
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(
        message: String
    ) {

        getSystemService(
            NotificationManager::class.java
        ).notify(
            NOTIFICATION_ID,
            createNotification(message)
        )
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    override fun onDestroy() {

        Log.w(
            TAG,
            "SERVICE onDestroy"
        )

        stopVoiceListening()
        destroyRecognizer()

        super.onDestroy()
    }
}
