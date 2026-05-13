package app.whisperkb.data.db.entity

data class PromptEntity(
    val id: Long = 0,
    val title: String,
    val body: String,
    val type: String,
)
