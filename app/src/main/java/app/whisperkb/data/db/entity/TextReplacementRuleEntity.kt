package app.whisperkb.data.db.entity

data class TextReplacementRuleEntity(
    val id: Long = 0,
    val find: String,
    val replace: String,
    val enabled: Boolean = true,
)
