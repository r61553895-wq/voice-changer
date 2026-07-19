package com.voicechanger.app.audio_engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voicechanger.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the native audio engine alive when the
 * app is backgrounded (e.g. user switches to a call/voice-chat app while
 * the live voice change stays active). Required on Android 10+ for
 * uninterrupted microphone access outside the foreground activity.
 */
@AndroidEntryPoint
class AudioEngineService : Service() {

    @Inject lateinit var audioEngine: AudioEngine

    companion object {
        const val CHANNEL_ID = "voice_engine_channel"
        const val NOTIFICATION_ID = 42
        const val ACTION_START = "com.voicechanger.app.action.START"
        const val ACTION_STOP = "com.voicechanger.app.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                audioEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                audioEngine.start()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
