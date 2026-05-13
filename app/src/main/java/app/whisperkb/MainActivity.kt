package app.whisperkb

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.whisperkb.history.HistoryStore
import app.whisperkb.provider.CloudProviderStore
import app.whisperkb.provider.PromptStore
import app.whisperkb.provider.ProviderSettingsActivity
import app.whisperkb.services.TranscriptionService

class MainActivity : ComponentActivity() {
    private lateinit var statusView: TextView
    private lateinit var resultView: TextView
    private lateinit var errorView: TextView
    private lateinit var providerSummaryView: TextView
    private lateinit var promptSummaryView: TextView
    private lateinit var historyView: TextView
    private lateinit var historySearchInput: EditText

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshState() }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()

        statusView = TextView(this)
        resultView = TextView(this)
        errorView = TextView(this)
        providerSummaryView = TextView(this)
        promptSummaryView = TextView(this)
        historyView = TextView(this)
        historySearchInput = EditText(this).apply { hint = "Search history" }

        setContentView(
            ScrollView(this).apply {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(32, 32, 32, 32)
                        addView(TextView(context).apply { text = "whisperkb" })
                        addView(statusView)
                        addView(resultView)
                        addView(errorView)
                        addView(providerSummaryView)
                        addView(promptSummaryView)
                        addView(Button(context).apply {
                            text = "Manage provider"
                            setOnClickListener {
                                startActivity(Intent(context, ProviderSettingsActivity::class.java))
                            }
                        })
                        addPromptInput(this)
                        addView(historySearchInput)
                        addView(Button(context).apply {
                            text = "Refresh history"
                            setOnClickListener { refreshState() }
                        })
                        addView(historyView)
                        addView(Button(context).apply {
                            text = "Start recording"
                            setOnClickListener { startServiceAction(TranscriptionService.ACTION_START_RECORDING) }
                        })
                        addView(Button(context).apply {
                            text = "Stop recording"
                            setOnClickListener { startServiceAction(TranscriptionService.ACTION_STOP_RECORDING) }
                        })
                        addView(Button(context).apply {
                            text = "Cancel"
                            setOnClickListener { startServiceAction(TranscriptionService.ACTION_CANCEL) }
                        })
                        addView(Button(context).apply {
                            text = "Retry"
                            setOnClickListener { startServiceAction(TranscriptionService.ACTION_RETRY) }
                        })
                        addView(Button(context).apply {
                            text = "Copy last text"
                            setOnClickListener { startServiceAction(TranscriptionService.ACTION_COPY_TEXT) }
                        })
                        addView(Button(context).apply {
                            text = "Accessibility settings"
                            setOnClickListener {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        })
                        addView(Button(context).apply {
                            text = "Input method settings"
                            setOnClickListener {
                                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            }
                        })
                    }
                )
            }
        )

        historySearchInput.setOnEditorActionListener { _, _, _ ->
            refreshState()
            false
        }
        refreshState()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(AppStateStore.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refreshState()
    }

    override fun onStop() {
        unregisterReceiver(stateReceiver)
        super.onStop()
    }

    private fun addPromptInput(parent: LinearLayout) {
        val promptInput = EditText(this).apply {
            hint = "Correction prompt"
            setLines(4)
            setText(PromptStore.load(this@MainActivity))
        }
        parent.addView(promptInput)
        parent.addView(Button(this).apply {
            text = "Save prompt"
            setOnClickListener {
                PromptStore.save(this@MainActivity, promptInput.text.toString())
                refreshState()
            }
        })
    }

    private fun startServiceAction(action: String) {
        val intent = Intent(this, TranscriptionService::class.java).apply { this.action = action }
        if (action == TranscriptionService.ACTION_START_RECORDING || action == TranscriptionService.ACTION_RESET_AND_START_RECORDING || action == TranscriptionService.ACTION_START_RECORDING_LONG_PRESS_TO_TALK) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        refreshState()
    }

    private fun requestPermissionsIfNeeded() {
        val required = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissions.launch(required)
    }

    private fun refreshState() {
        val snapshot = AppStateStore.snapshot(this)
        val provider = CloudProviderStore.load(this)
        val historyQuery = historySearchInput.text?.toString().orEmpty()
        val history = HistoryStore.search(this, historyQuery).take(10)
        statusView.text = "State: ${snapshot.state.name.lowercase()}"
        resultView.text = "Transcript: ${snapshot.transcript.orEmpty()}"
        errorView.text = "Error: ${snapshot.error.orEmpty()}"
        providerSummaryView.text = if (provider == null) {
            "Provider: none"
        } else {
            "Provider: ${provider.name} | ${provider.endpoint} | ${provider.model}"
        }
        promptSummaryView.text = "Prompt: ${PromptStore.load(this)}"
        historyView.text = if (history.isEmpty()) {
            "History: none"
        } else {
            buildString {
                appendLine("History:")
                history.forEach {
                    appendLine("- #${it.id} ${it.createdAt}: ${it.finalText.ifBlank { it.rawText }}")
                    if (!it.providerName.isNullOrBlank()) appendLine("  provider=${it.providerName} model=${it.model.orEmpty()}")
                    if (!it.error.isNullOrBlank()) appendLine("  error=${it.error}")
                }
            }
        }
    }
}
