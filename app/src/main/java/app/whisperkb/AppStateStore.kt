package app.whisperkb

import android.content.Context
import android.content.Intent

private const val PREFS_NAME = "whisperkb_state"
private const val KEY_STATE = "state"
private const val KEY_TRANSCRIPT = "transcript"
private const val KEY_ERROR = "error"
private const val KEY_AUDIO_PATH = "audio_path"
private const val KEY_UPDATED_AT = "updated_at"
private const val KEY_INSERTED_AT = "inserted_at"

enum class ServiceState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    COMPLETED,
    ERROR,
}

data class ServiceSnapshot(
    val state: ServiceState,
    val transcript: String?,
    val error: String?,
    val audioPath: String?,
    val updatedAt: Long,
    val insertedAt: Long,
)

object AppStateStore {
    const val ACTION_STATE_CHANGED = "app.whisperkb.STATE_CHANGED"

    fun snapshot(context: Context): ServiceSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ServiceSnapshot(
            state = runCatching { ServiceState.valueOf(prefs.getString(KEY_STATE, ServiceState.IDLE.name) ?: ServiceState.IDLE.name) }
                .getOrDefault(ServiceState.IDLE),
            transcript = prefs.getString(KEY_TRANSCRIPT, null),
            error = prefs.getString(KEY_ERROR, null),
            audioPath = prefs.getString(KEY_AUDIO_PATH, null),
            updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L),
            insertedAt = prefs.getLong(KEY_INSERTED_AT, 0L),
        )
    }

    fun update(
        context: Context,
        state: ServiceState? = null,
        transcript: String? = null,
        error: String? = null,
        audioPath: String? = null,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (state != null) putString(KEY_STATE, state.name)
                if (transcript != null) putString(KEY_TRANSCRIPT, transcript) else remove(KEY_TRANSCRIPT)
                if (error != null) putString(KEY_ERROR, error) else remove(KEY_ERROR)
                if (audioPath != null) putString(KEY_AUDIO_PATH, audioPath) else remove(KEY_AUDIO_PATH)
                putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                if (transcript != null) putLong(KEY_INSERTED_AT, 0L)
                apply()
            }
        context.sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(context.packageName))
    }

    fun clearResult(context: Context) {
        update(context, transcript = null, error = null)
    }

    fun markInserted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_INSERTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun setIdle(context: Context) {
        update(context, state = ServiceState.IDLE)
    }
}
