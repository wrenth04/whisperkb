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
        val action = when (snapshot.state) {
            ServiceState.RECORDING, ServiceState.TRANSCRIBING -> TranscriptionService.ACTION_STOP_RECORDING
            ServiceState.ERROR -> TranscriptionService.ACTION_RETRY
            else -> TranscriptionService.ACTION_START_RECORDING
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
            ServiceState.COMPLETED -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        qsTile?.label = when (snapshot.state) {
            ServiceState.RECORDING -> "Stop whisperkb"
            ServiceState.TRANSCRIBING -> "Transcribing"
            ServiceState.ERROR -> "Retry whisperkb"
            ServiceState.COMPLETED -> "Copy whisperkb"
            else -> "Record"
        }
        qsTile?.subtitle = when (snapshot.state) {
            ServiceState.COMPLETED -> snapshot.transcript?.take(24)
            ServiceState.ERROR -> snapshot.error
            else -> null
        }
        qsTile?.updateTile()
    }
}
