package app.whisperkb.quicktile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import app.whisperkb.AppStateStore
import app.whisperkb.ServiceState
import app.whisperkb.services.TranscriptionService

class QuickRecordTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val snapshot = AppStateStore.snapshot(this)
        val action = if (snapshot.state == ServiceState.RECORDING || snapshot.state == ServiceState.TRANSCRIBING) {
            TranscriptionService.ACTION_STOP_RECORDING
        } else {
            TranscriptionService.ACTION_START_RECORDING
        }
        val intent = Intent(this, TranscriptionService::class.java).apply { this.action = action }
        if (action == TranscriptionService.ACTION_START_RECORDING) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        refreshTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    private fun refreshTile() {
        val snapshot = AppStateStore.snapshot(this)
        qsTile?.state = when (snapshot.state) {
            ServiceState.RECORDING, ServiceState.TRANSCRIBING -> Tile.STATE_ACTIVE
            ServiceState.ERROR -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        qsTile?.label = when (snapshot.state) {
            ServiceState.RECORDING -> "Stop whisperkb"
            ServiceState.TRANSCRIBING -> "Transcribing"
            ServiceState.ERROR -> "whisperkb error"
            else -> "Record"
        }
        qsTile?.updateTile()
    }
}
