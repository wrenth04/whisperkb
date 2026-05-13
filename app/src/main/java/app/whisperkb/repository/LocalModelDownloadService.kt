package app.whisperkb.repository

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.whisperkb.MainActivity
import app.whisperkb.WhisperkbApplication
import java.io.File

class LocalModelDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Preparing local model"))
        when (intent?.action) {
            ACTION_DOWNLOAD, null -> installPlaceholderModel()
            ACTION_CANCEL -> stopSelf()
        }
        return START_STICKY
    }

    private fun installPlaceholderModel() {
        val targetDir = File(applicationContext.filesDir, "models/local").apply { mkdirs() }
        val tokens = File(targetDir, "tokens.txt")
        val model = File(targetDir, "model.int8.onnx")
        if (!tokens.exists()) tokens.writeText("<unk>\n")
        if (!model.exists()) model.writeText("placeholder")
        startForeground(NOTIFICATION_ID, buildNotification("Local model ready"))
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, WhisperkbApplication.CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("whisperkb")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_CANCEL = "whisperkb.localmodel.CANCEL"
        const val ACTION_DOWNLOAD = "whisperkb.localmodel.DOWNLOAD"
        private const val NOTIFICATION_ID = 2001
    }
}
