package app.whisperkb.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.whisperkb.services.TranscriptionService

class RecorderWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            ACTION_START -> TranscriptionService.ACTION_START_RECORDING
            ACTION_STOP -> TranscriptionService.ACTION_STOP_RECORDING
            ACTION_RETRY -> TranscriptionService.ACTION_RETRY
            else -> return
        }
        val serviceIntent = Intent(context, TranscriptionService::class.java).apply { this.action = action }
        if (action == TranscriptionService.ACTION_START_RECORDING) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_START = "whisperkb.widget.ACTION_START"
        const val ACTION_STOP = "whisperkb.widget.ACTION_STOP"
        const val ACTION_RETRY = "whisperkb.widget.ACTION_RETRY"
    }
}
