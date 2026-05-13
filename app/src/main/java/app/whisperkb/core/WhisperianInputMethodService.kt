package app.whisperkb.core

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.whisperkb.AppStateStore
import app.whisperkb.MainActivity
import app.whisperkb.ServiceState
import app.whisperkb.services.TranscriptionService

class WhisperkbInputMethodService : InputMethodService() {
    private lateinit var statusView: TextView
    private lateinit var toggleButton: Button
    private var lastCommittedAt = 0L

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            refreshState()
            commitPendingTranscript()
        }
    }

    override fun onCreateInputView(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        statusView = TextView(context)
        toggleButton = Button(context)
        toggleButton.setOnClickListener { toggleRecording() }
        addView(statusView)
        addView(toggleButton)
        addView(Button(context).apply {
            text = "Open app"
            setOnClickListener { startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        })
        refreshState()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        registerReceiver(stateReceiver, IntentFilter(AppStateStore.ACTION_STATE_CHANGED))
        refreshState()
        commitPendingTranscript()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        unregisterReceiver(stateReceiver)
        super.onFinishInputView(finishingInput)
    }

    private fun toggleRecording() {
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
        refreshState()
    }

    private fun refreshState() {
        val snapshot = AppStateStore.snapshot(this)
        statusView.text = when (snapshot.state) {
            ServiceState.RECORDING -> "State: recording"
            ServiceState.TRANSCRIBING -> "State: transcribing"
            ServiceState.COMPLETED -> "State: ready to insert"
            ServiceState.ERROR -> "State: error"
            ServiceState.IDLE -> "State: idle"
        }
        toggleButton.text = if (snapshot.state == ServiceState.RECORDING || snapshot.state == ServiceState.TRANSCRIBING) "Stop recording" else "Record"
    }

    private fun commitPendingTranscript() {
        val snapshot = AppStateStore.snapshot(this)
        val transcript = snapshot.transcript
        if (snapshot.state != ServiceState.COMPLETED || transcript.isNullOrBlank() || snapshot.updatedAt <= lastCommittedAt) return
        currentInputConnection?.commitText(transcript, 1)
        lastCommittedAt = snapshot.updatedAt
    }
}
