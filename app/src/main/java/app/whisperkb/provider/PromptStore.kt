package app.whisperkb.provider

import android.content.Context
import androidx.room.Room
import app.whisperkb.data.db.WhisperkbDatabase
import app.whisperkb.data.db.entity.PromptEntity
import kotlinx.coroutines.runBlocking

private const val PREFS_NAME = "whisperkb_prompt"
private const val KEY_PROMPT = "prompt"

object PromptStore {
    fun load(context: Context): String = runCatching {
        runBlocking { database(context).dao().listPrompts() }.firstOrNull()?.body ?: DEFAULT_PROMPT
    }.getOrElse {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROMPT, DEFAULT_PROMPT)
            .orEmpty()
    }

    fun save(context: Context, prompt: String) {
        runCatching {
            runBlocking {
                database(context).dao().insertPrompt(PromptEntity(title = "Default", body = prompt, type = "correction"))
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROMPT, prompt)
            .apply()
    }

    const val DEFAULT_PROMPT = "Correct the transcription for spelling, punctuation, and grammar while preserving the original meaning."

    private fun database(context: Context): WhisperkbDatabase = Room.databaseBuilder(
        context.applicationContext,
        WhisperkbDatabase::class.java,
        WhisperkbDatabase.DATABASE_NAME,
    ).build()
}
