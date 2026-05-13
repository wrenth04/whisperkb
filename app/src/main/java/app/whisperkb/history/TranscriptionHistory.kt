package app.whisperkb.history

data class TranscriptionHistoryEntry(
    val id: Long,
    val createdAt: Long,
    val rawText: String,
    val finalText: String,
    val providerName: String?,
    val model: String?,
    val prompt: String?,
    val error: String?,
    val audioPath: String?,
)
