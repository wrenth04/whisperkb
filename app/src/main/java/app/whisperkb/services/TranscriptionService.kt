package app.whisperkb.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.whisperkb.AppStateStore
import app.whisperkb.MainActivity
import app.whisperkb.ServiceState
import app.whisperkb.WhisperkbApplication
import app.whisperkb.localmodels.LocalSherpaOnnxEngine
import app.whisperkb.history.HistoryStore
import app.whisperkb.provider.CloudProviderClient
import app.whisperkb.provider.CloudProviderStore
import app.whisperkb.provider.PromptStore
import java.io.File
import kotlin.concurrent.thread

class TranscriptionService : Service() {
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var engine: LocalSherpaOnnxEngine? = null
    private var transcribing = false
    private var lastTranscript: String? = null

    override fun onCreate() {
        super.onCreate()
        engine = LocalSherpaOnnxEngine(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING,
            ACTION_RESET_AND_START_RECORDING,
            ACTION_START_RECORDING_LONG_PRESS_TO_TALK -> startRecording()

            ACTION_STOP_RECORDING,
            ACTION_STOP_RECORDING_LONG_PRESS_TO_TALK -> stopRecordingAndTranscribe()

            ACTION_CANCEL -> cancelRecording()
            ACTION_COPY_TEXT -> copyLastTranscript()
            ACTION_RETRY -> retryLastRecording()
            ACTION_TRANSCRIPTION_RESULT -> applyTranscriptionResult(intent)
            ACTION_PAUSE,
            ACTION_RESUME,
            ACTION_PROFILE_NEXT,
            ACTION_PROFILE_PREVIOUS,
            ACTION_DISMISS_CONTINUE -> Unit
            null -> syncForegroundState()
            else -> syncForegroundState()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseRecorder()
        super.onDestroy()
    }

    private fun startRecording() {
        if (transcribing || recorder != null) return

        val file = createRecordingFile()
        recordingFile = file
        lastTranscript = null
        AppStateStore.update(this, state = ServiceState.RECORDING, transcript = null, error = null, audioPath = file.absolutePath)
        startForeground(NOTIFICATION_ID, buildNotification("Recording started", allowStop = true, allowCancel = true))

        runCatching {
            recorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(16_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.onFailure { throwable ->
            releaseRecorder()
            AppStateStore.update(
                this,
                state = ServiceState.ERROR,
                error = throwable.message ?: "Recording failed",
                audioPath = file.absolutePath,
            )
            startForeground(NOTIFICATION_ID, buildNotification("Recording failed", allowStop = false, allowCancel = false, allowRetry = true, allowCopy = false))
        }
    }

    private fun stopRecordingAndTranscribe() {
        val file = recordingFile
        if (file == null) {
            AppStateStore.update(this, state = ServiceState.IDLE)
            stopSelf()
            return
        }

        val recorder = recorder
        releaseRecorder()
        transcribing = true
        AppStateStore.update(this, state = ServiceState.TRANSCRIBING, error = null, audioPath = file.absolutePath)
        startForeground(NOTIFICATION_ID, buildNotification("Transcribing", allowStop = false, allowCancel = true, allowRetry = false, allowCopy = false))

        thread(name = "whisperkb-transcribe") {
            val rawResult = engine?.transcribe(file.absolutePath)
                ?: Result.failure(IllegalStateException("Transcription engine unavailable"))

            rawResult
                .onSuccess { transcript ->
                    val provider = CloudProviderStore.load(this)
                    val prompt = PromptStore.load(this)
                    val corrected = correctWithProvider(transcript)
                    val finalText = corrected.getOrElse { transcript }
                    lastTranscript = finalText
                    val correctionError = corrected.exceptionOrNull()?.message
                    HistoryStore.add(
                        context = this,
                        rawText = transcript,
                        finalText = finalText,
                        providerName = provider?.name,
                        model = provider?.model,
                        prompt = prompt,
                        error = correctionError,
                        audioPath = file.absolutePath,
                    )
                    AppStateStore.update(
                        this,
                        state = ServiceState.COMPLETED,
                        transcript = finalText,
                        error = correctionError,
                        audioPath = file.absolutePath,
                    )
                    startForeground(NOTIFICATION_ID, buildNotification("Transcription ready", allowStop = false, allowCancel = false, allowRetry = true, allowCopy = finalText.isNotBlank()))
                }
                .onFailure { throwable ->
                    val error = throwable.message ?: "Transcription failed"
                    AppStateStore.update(
                        this,
                        state = ServiceState.ERROR,
                        error = error,
                        audioPath = file.absolutePath,
                    )
                    startForeground(NOTIFICATION_ID, buildNotification(error, allowStop = false, allowCancel = false, allowRetry = true, allowCopy = false))
                }

            transcribing = false
        }
    }

    private fun cancelRecording() {
        releaseRecorder()
        recordingFile?.delete()
        recordingFile = null
        transcribing = false
        lastTranscript = null
        AppStateStore.update(this, state = ServiceState.IDLE, transcript = null, error = null, audioPath = null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun retryLastRecording() {
        val file = recordingFile
        if (file != null && file.exists()) {
            stopRecordingAndTranscribe()
        } else {
            startRecording()
        }
    }

    private fun copyLastTranscript() {
        val transcript = lastTranscript ?: AppStateStore.snapshot(this).transcript
        if (transcript.isNullOrBlank()) return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("whisperkb", transcript))
        startForeground(NOTIFICATION_ID, buildNotification("Copied to clipboard", allowStop = false, allowCancel = false, allowRetry = true, allowCopy = true))
    }

    private fun applyTranscriptionResult(intent: Intent) {
        val transcript = intent.getStringExtra(EXTRA_TRANSCRIPT)
        val error = intent.getStringExtra(EXTRA_ERROR)
        when {
            !transcript.isNullOrBlank() -> {
                lastTranscript = transcript
                AppStateStore.update(this, state = ServiceState.COMPLETED, transcript = transcript, error = null)
            }
            !error.isNullOrBlank() -> {
                AppStateStore.update(this, state = ServiceState.ERROR, error = error)
            }
        }
    }

    private fun correctWithProvider(rawText: String): Result<String> {
        val provider = CloudProviderStore.load(this) ?: return Result.success(rawText)
        val prompt = PromptStore.load(this)
        return CloudProviderClient(provider).correctText(rawText, prompt)
    }

    private fun syncForegroundState() {
        when (AppStateStore.snapshot(this).state) {
            ServiceState.RECORDING -> startForeground(NOTIFICATION_ID, buildNotification("Recording", allowStop = true, allowCancel = true))
            ServiceState.TRANSCRIBING -> startForeground(NOTIFICATION_ID, buildNotification("Transcribing", allowStop = false, allowCancel = true))
            ServiceState.COMPLETED -> startForeground(NOTIFICATION_ID, buildNotification("Transcription ready", allowStop = false, allowCancel = false, allowRetry = true, allowCopy = true))
            ServiceState.ERROR -> startForeground(NOTIFICATION_ID, buildNotification(AppStateStore.snapshot(this).error ?: "Transcription failed", allowStop = false, allowCancel = false, allowRetry = true, allowCopy = false))
            ServiceState.IDLE -> Unit
        }
    }

    private fun buildNotification(
        text: String,
        allowStop: Boolean,
        allowCancel: Boolean,
        allowRetry: Boolean = false,
        allowCopy: Boolean = false,
    ): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, WhisperkbApplication.CHANNEL_RECORDING)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("whisperkb")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(allowStop)
            .apply {
                if (allowStop) {
                    addAction(action("Stop", ACTION_STOP_RECORDING, 1))
                }
                if (allowCancel) {
                    addAction(action("Cancel", ACTION_CANCEL, 2))
                }
                if (allowRetry) {
                    addAction(action("Retry", ACTION_RETRY, 3))
                }
                if (allowCopy) {
                    addAction(action("Copy", ACTION_COPY_TEXT, 4))
                }
            }
            .build()
    }

    private fun action(label: String, action: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(this, TranscriptionService::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action.Builder(android.R.drawable.ic_menu_manage, label, pendingIntent).build()
    }

    private fun createRecordingFile(): File {
        val dir = File(cacheDir, "recordings").apply { mkdirs() }
        return File(dir, "recording_${System.currentTimeMillis()}.m4a")
    }

    private fun releaseRecorder() {
        recorder?.runCatching {
            runCatching { stop() }
            runCatching { reset() }
            runCatching { release() }
        }
        recorder = null
    }

    companion object {
        const val ACTION_CANCEL = "whisperkb.CANCEL"
        const val ACTION_COPY_TEXT = "whisperkb.COPY_TEXT"
        const val ACTION_DISMISS_CONTINUE = "whisperkb.DISMISS_CONTINUE"
        const val ACTION_PAUSE = "whisperkb.PAUSE"
        const val ACTION_PROFILE_NEXT = "whisperkb.PROFILE_NEXT"
        const val ACTION_PROFILE_PREVIOUS = "whisperkb.PROFILE_PREVIOUS"
        const val ACTION_RESET_AND_START_RECORDING = "whisperkb.RESET_AND_START_RECORDING"
        const val ACTION_RESUME = "whisperkb.RESUME"
        const val ACTION_RETRY = "whisperkb.RETRY"
        const val ACTION_START_RECORDING = "whisperkb.START_RECORDING"
        const val ACTION_START_RECORDING_LONG_PRESS_TO_TALK = "whisperkb.START_RECORDING_LONG_PRESS_TO_TALK"
        const val ACTION_STOP_RECORDING = "whisperkb.STOP_RECORDING"
        const val ACTION_STOP_RECORDING_LONG_PRESS_TO_TALK = "whisperkb.STOP_RECORDING_LONG_PRESS_TO_TALK"
        const val ACTION_TRANSCRIPTION_RESULT = "whisperkb.TRANSCRIPTION_RESULT"
        const val EXTRA_TRANSCRIPT = "extra_transcript"
        const val EXTRA_ERROR = "extra_error"
        private const val NOTIFICATION_ID = 1001
    }
}
