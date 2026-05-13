package app.whisperkb.core

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.whisperkb.AppStateStore
import app.whisperkb.MainActivity
import app.whisperkb.R
import app.whisperkb.ServiceState
import app.whisperkb.services.TranscriptionService

class WhisperkbInputMethodService : InputMethodService() {
    private lateinit var titleView: TextView
    private lateinit var statusView: TextView
    private lateinit var mainActionButton: Button
    private lateinit var secondaryButton1: Button
    private lateinit var secondaryButton2: Button
    private lateinit var utilityButton1: Button
    private lateinit var utilityButton2: Button
    private lateinit var utilityButton3: Button
    private lateinit var utilityButton4: Button
    private var lastInsertedAt = 0L
    private var receiverRegistered = false
    private var currentEditorInfo: EditorInfo? = null

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            refreshState()
            commitPendingTranscript()
        }
    }

    override fun onCreateInputView(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = roundedBackground(resources.getColor(R.color.ime_background, theme))

        addView(buildTopRow())
        addView(buildMainActionRow())
        addView(buildSecondaryRow())
        addView(buildUtilityRow())
        minimumHeight = dp(248)
        refreshState()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                stateReceiver,
                IntentFilter(AppStateStore.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        refreshState()
        commitPendingTranscript()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun buildTopRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(6))
        titleView = TextView(context).apply {
            text = getString(R.string.ime_title)
            textSize = 15f
            setTextColor(resources.getColor(R.color.ime_on_surface, theme))
        }
        statusView = TextView(context).apply {
            textSize = 12f
            setTextColor(resources.getColor(R.color.ime_disabled, theme))
        }
        addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(statusView)
    }

    private fun buildMainActionRow(): View = LinearLayout(this).apply {
        setPadding(0, dp(4), 0, dp(6))
        mainActionButton = createActionButton()
        addView(mainActionButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)))
    }

    private fun buildSecondaryRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(2), 0, dp(2))
        secondaryButton1 = createSmallButton()
        secondaryButton2 = createSmallButton()
        addView(secondaryButton1, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(8) })
        addView(secondaryButton2, LinearLayout.LayoutParams(0, dp(42), 1f))
    }

    private fun buildUtilityRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(6), 0, 0)
        utilityButton1 = createSmallButton()
        utilityButton2 = createSmallButton()
        utilityButton3 = createSmallButton()
        utilityButton4 = createSmallButton()
        addView(utilityButton1, utilitySlot())
        addView(utilityButton2, utilitySlot())
        addView(utilityButton3, utilitySlot())
        addView(utilityButton4, utilitySlot(last = true))
    }

    private fun utilitySlot(last: Boolean = false): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
        if (!last) marginEnd = dp(6)
    }

    private fun createActionButton(): Button = Button(this).apply {
        textSize = 17f
        setAllCaps(false)
        isAllCaps = false
        isSingleLine = true
        gravity = Gravity.CENTER
        minHeight = dp(72)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = roundedBackground(resources.getColor(R.color.ime_primary, theme))
        setTextColor(resources.getColor(R.color.ime_on_primary, theme))
    }

    private fun createSmallButton(): Button = Button(this).apply {
        textSize = 14f
        setAllCaps(false)
        isAllCaps = false
        isSingleLine = true
        gravity = Gravity.CENTER
        minHeight = dp(40)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = roundedBackground(resources.getColor(R.color.ime_secondary_surface, theme))
        setTextColor(resources.getColor(R.color.ime_on_surface, theme))
    }

    private fun roundedBackground(color: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(color)
    }

    private fun refreshState() {
        val snapshot = AppStateStore.snapshot(this)
        titleView.text = getString(R.string.ime_title)
        statusView.text = when (snapshot.state) {
            ServiceState.RECORDING -> getString(R.string.ime_status_recording)
            ServiceState.TRANSCRIBING -> getString(R.string.ime_status_transcribing)
            ServiceState.COMPLETED -> getString(R.string.ime_status_completed)
            ServiceState.ERROR -> snapshot.error ?: getString(R.string.ime_status_error)
            ServiceState.IDLE -> getString(R.string.ime_status_idle)
        }

        when (snapshot.state) {
            ServiceState.IDLE -> {
                setMainAction(getString(R.string.ime_action_start), true) { toggleRecording() }
                setSecondary(getString(R.string.ime_action_insert_last), snapshot.transcript != null) { commitPendingTranscript(force = true) }
                setSecondaryRight(getString(R.string.ime_action_open_app), true) { openApp() }
                setUtilities()
            }
            ServiceState.RECORDING -> {
                setMainAction(getString(R.string.ime_action_stop), true) { toggleRecording() }
                setSecondary(getString(R.string.ime_action_cancel), true) { startServiceAction(TranscriptionService.ACTION_CANCEL) }
                setSecondaryRight(getString(R.string.ime_action_pause), false) { startServiceAction(TranscriptionService.ACTION_PAUSE) }
                setUtilities()
            }
            ServiceState.TRANSCRIBING -> {
                setMainAction(getString(R.string.ime_status_transcribing), false, true)
                setSecondary(getString(R.string.ime_action_cancel), true) { startServiceAction(TranscriptionService.ACTION_CANCEL) }
                setSecondaryRight(getString(R.string.ime_action_open_app), true) { openApp() }
                setUtilities()
            }
            ServiceState.COMPLETED -> {
                setMainAction(getString(R.string.ime_action_insert), true) { commitPendingTranscript(force = true) }
                setSecondary(getString(R.string.ime_action_done), true) { hideWindow() }
                setSecondaryRight(getString(R.string.ime_action_insert_last), true) { commitPendingTranscript(force = true) }
                setUtilities()
            }
            ServiceState.ERROR -> {
                setMainAction(getString(R.string.ime_action_retry), true) { startServiceAction(TranscriptionService.ACTION_RETRY) }
                setSecondary(getString(R.string.ime_action_cancel), true) { startServiceAction(TranscriptionService.ACTION_CANCEL) }
                setSecondaryRight(getString(R.string.ime_action_open_app), true) { openApp() }
                setUtilities()
            }
        }
    }

    private fun setMainAction(text: String, enabled: Boolean, disabled: Boolean = false, onClick: (() -> Unit)? = null) {
        mainActionButton.text = text
        mainActionButton.isEnabled = enabled && !disabled
        mainActionButton.setOnClickListener(if (enabled && !disabled && onClick != null) View.OnClickListener { onClick() } else null)
        mainActionButton.background = roundedBackground(
            if (enabled && !disabled) resources.getColor(R.color.ime_primary, theme) else resources.getColor(R.color.ime_secondary_surface, theme)
        )
        mainActionButton.setTextColor(
            if (enabled && !disabled) resources.getColor(R.color.ime_on_primary, theme) else resources.getColor(R.color.ime_disabled, theme)
        )
    }

    private fun setSecondary(text: String, enabled: Boolean, onClick: () -> Unit) {
        secondaryButton1.text = text
        secondaryButton1.isEnabled = enabled
        secondaryButton1.setOnClickListener(if (enabled) View.OnClickListener { onClick() } else null)
    }

    private fun setSecondaryRight(text: String, enabled: Boolean, onClick: () -> Unit) {
        secondaryButton2.text = text
        secondaryButton2.isEnabled = enabled
        secondaryButton2.setOnClickListener(if (enabled) View.OnClickListener { onClick() } else null)
    }

    private fun setUtilities() {
        utilityButton1.text = getString(R.string.ime_action_hide_keyboard)
        utilityButton1.setOnClickListener { requestHideSelf(0) }
        utilityButton2.text = getString(R.string.ime_action_return)
        utilityButton2.setOnClickListener { sendReturn() }
        utilityButton3.text = getString(R.string.ime_action_switch_keyboard)
        utilityButton3.setOnClickListener { switchKeyboard() }
        utilityButton4.text = getString(R.string.ime_action_profiles)
        utilityButton4.setOnClickListener { openApp() }
    }

    private fun toggleRecording() {
        val snapshot = AppStateStore.snapshot(this)
        val action = if (snapshot.state == ServiceState.RECORDING || snapshot.state == ServiceState.TRANSCRIBING) {
            TranscriptionService.ACTION_STOP_RECORDING
        } else {
            TranscriptionService.ACTION_START_RECORDING
        }
        startServiceAction(action)
    }

    private fun startServiceAction(action: String) {
        val intent = Intent(this, TranscriptionService::class.java).apply { this.action = action }
        if (action == TranscriptionService.ACTION_START_RECORDING) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        refreshState()
    }

    private fun sendReturn() {
        val editorInfo = currentEditorInfo
        val actionId = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: 0
        if (actionId != 0) {
            currentInputConnection?.performEditorAction(actionId)
            return
        }
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun switchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchToNextInputMethod(false)
        } else {
            openApp()
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun commitPendingTranscript(force: Boolean = false) {
        val snapshot = AppStateStore.snapshot(this)
        val transcript = snapshot.transcript
        val insertedAt = snapshot.insertedAt
        if (snapshot.state != ServiceState.COMPLETED || transcript.isNullOrBlank()) return
        if (!force && snapshot.updatedAt <= lastInsertedAt) return
        if (!force && insertedAt >= snapshot.updatedAt) return
        currentInputConnection?.commitText(transcript, 1)
        lastInsertedAt = snapshot.updatedAt
        AppStateStore.markInserted(this)
        refreshState()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
