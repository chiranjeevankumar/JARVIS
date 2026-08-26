package com.chiranjeevankumar.aura

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * AURA v0.7 background voice service.
 *
 * This restoration keeps the service minimal and safe.
 * Voice behavior can be extended in later checkpoints.
 */
class AuraVoiceService : Service() {

    companion object {

        const val ACTION_START_WAKE =
            "com.chiranjeevankumar.aura.ACTION_START_WAKE"

        const val ACTION_STOP_WAKE =
            "com.chiranjeevankumar.aura.ACTION_STOP_WAKE"

        private const val CHANNEL_ID =
            "aura_voice_service"

        private const val NOTIFICATION_ID =
            7001
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START_WAKE -> {
                // Wake/voice implementation continues
                // in the next verified checkpoint.
            }

            ACTION_STOP_WAKE -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "AURA Voice",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AURA")
                .setContentText("AURA voice service is running")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("AURA")
                .setContentText("AURA voice service is running")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        }
    }
}
