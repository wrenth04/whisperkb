package app.whisperkb.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import app.whisperkb.data.db.entity.ProfileEntity
import app.whisperkb.data.db.entity.PromptEntity
import app.whisperkb.data.db.entity.TextReplacementRuleEntity
import app.whisperkb.data.db.entity.TranscriptionEntity

@Dao
interface WhisperkbDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    suspend fun listTranscriptions(): List<TranscriptionEntity>

    @Insert
    suspend fun insertTranscription(entity: TranscriptionEntity): Long

    @Query("DELETE FROM transcriptions")
    suspend fun clearTranscriptions()

    @Query("SELECT * FROM prompts ORDER BY id DESC")
    suspend fun listPrompts(): List<PromptEntity>

    @Insert
    suspend fun insertPrompt(entity: PromptEntity): Long

    @Query("SELECT * FROM profiles ORDER BY id DESC")
    suspend fun listProfiles(): List<ProfileEntity>

    @Insert
    suspend fun insertProfile(entity: ProfileEntity): Long

    @Query("SELECT * FROM text_replacement_rules ORDER BY id DESC")
    suspend fun listReplacementRules(): List<TextReplacementRuleEntity>

    @Insert
    suspend fun insertReplacementRule(entity: TextReplacementRuleEntity): Long

    @Delete
    suspend fun deleteReplacementRule(entity: TextReplacementRuleEntity)
}
