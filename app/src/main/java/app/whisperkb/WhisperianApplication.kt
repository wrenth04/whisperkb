package app.whisperkb

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WhisperkbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                CHANNEL_RECORDING,
                "Recording",
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannels(channels)
    }

    companion object {
        const val CHANNEL_RECORDING = "whisperkb_recording"
        const val CHANNEL_DOWNLOAD = "whisperkb_download"
    }
}
