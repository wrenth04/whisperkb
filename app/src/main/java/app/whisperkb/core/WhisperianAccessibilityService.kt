package app.whisperkb.core

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class WhisperkbAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
