package app.whisperkb.repository

import android.app.Service
import android.content.Intent
import android.os.IBinder

class LocalModelDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
