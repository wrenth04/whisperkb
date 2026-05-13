package app.whisperkb.data.db.entity

data class ProfileEntity(
    val id: Long = 0,
    val name: String,
    val provider: String,
    val model: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
