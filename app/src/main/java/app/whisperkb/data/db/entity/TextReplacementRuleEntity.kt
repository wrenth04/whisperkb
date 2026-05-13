package app.whisperkb.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_replacement_rules")
data class TextReplacementRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val find: String,
    val replace: String,
    val enabled: Boolean = true,
)
