package app.whisperkb.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.whisperkb.data.db.entity.ProfileEntity
import app.whisperkb.data.db.entity.PromptEntity
import app.whisperkb.data.db.entity.TextReplacementRuleEntity
import app.whisperkb.data.db.entity.TranscriptionEntity

@Database(
    entities = [
        ProfileEntity::class,
        PromptEntity::class,
        TextReplacementRuleEntity::class,
        TranscriptionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WhisperkbDatabase : RoomDatabase() {
    abstract fun dao(): WhisperkbDao

    companion object {
        const val DATABASE_NAME = "whisperkb.db"
    }
}
