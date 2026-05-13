package app.whisperkb.history

import android.content.Context

private const val PREFS_NAME = "whisperkb_history"
private const val KEY_NEXT_ID = "next_id"
private const val KEY_ENTRIES = "entries"

object HistoryStore {
    fun list(context: Context): List<TranscriptionHistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, "")
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("")
            .filter { it.isNotBlank() }
            .mapNotNull { deserialize(it) }
            .sortedByDescending { it.createdAt }
    }

    fun add(
        context: Context,
        rawText: String,
        finalText: String,
        providerName: String? = null,
        model: String? = null,
        prompt: String? = null,
        error: String? = null,
        audioPath: String? = null,
    ): TranscriptionHistoryEntry {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextId = prefs.getLong(KEY_NEXT_ID, 1L)
        val entry = TranscriptionHistoryEntry(
            id = nextId,
            createdAt = System.currentTimeMillis(),
            rawText = rawText,
            finalText = finalText,
            providerName = providerName,
            model = model,
            prompt = prompt,
            error = error,
            audioPath = audioPath,
        )
        val updated = list(context).plus(entry)
        prefs.edit()
            .putLong(KEY_NEXT_ID, nextId + 1)
            .putString(KEY_ENTRIES, updated.joinToString("") { serialize(it) })
            .apply()
        return entry
    }

    fun search(context: Context, query: String): List<TranscriptionHistoryEntry> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return list(context)
        return list(context).filter {
            it.rawText.contains(trimmed, ignoreCase = true) ||
                it.finalText.contains(trimmed, ignoreCase = true) ||
                (it.providerName?.contains(trimmed, ignoreCase = true) == true) ||
                (it.model?.contains(trimmed, ignoreCase = true) == true)
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ENTRIES)
            .apply()
    }

    private fun serialize(entry: TranscriptionHistoryEntry): String = listOf(
        entry.id.toString(),
        entry.createdAt.toString(),
        entry.rawText.encode(),
        entry.finalText.encode(),
        entry.providerName.orEmpty().encode(),
        entry.model.orEmpty().encode(),
        entry.prompt.orEmpty().encode(),
        entry.error.orEmpty().encode(),
        entry.audioPath.orEmpty().encode(),
    ).joinToString("|")

    private fun deserialize(value: String): TranscriptionHistoryEntry? {
        val parts = value.split("|")
        if (parts.size != 9) return null
        return runCatching {
            TranscriptionHistoryEntry(
                id = parts[0].toLong(),
                createdAt = parts[1].toLong(),
                rawText = parts[2].decode(),
                finalText = parts[3].decode(),
                providerName = parts[4].decode().ifBlank { null },
                model = parts[5].decode().ifBlank { null },
                prompt = parts[6].decode().ifBlank { null },
                error = parts[7].decode().ifBlank { null },
                audioPath = parts[8].decode().ifBlank { null },
            )
        }.getOrNull()
    }

    private fun String.encode(): String = replace("|", "%7C").replace("", "%1E")
    private fun String.decode(): String = replace("%7C", "|").replace("%1E", "")
}
