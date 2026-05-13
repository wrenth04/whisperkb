package app.whisperkb.core

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import app.whisperkb.AppStateStore
import app.whisperkb.ServiceState

class WhisperkbAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val snapshot = AppStateStore.snapshot(this)
        if (snapshot.state != ServiceState.COMPLETED || snapshot.transcript.isNullOrBlank()) return
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED && event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        Toast.makeText(this, "whisperkb transcript ready to insert", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() = Unit
}
