package app.whisperkb.data.db.entity

data class TranscriptionEntity(
    val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMillis: Long? = null,
    val profileId: Long? = null,
)
