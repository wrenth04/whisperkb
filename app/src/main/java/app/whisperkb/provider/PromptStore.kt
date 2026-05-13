package app.whisperkb.provider

import android.content.Context

private const val PREFS_NAME = "whisperkb_prompt"
private const val KEY_PROMPT = "prompt"

object PromptStore {
    fun load(context: Context): String = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_PROMPT, DEFAULT_PROMPT)
        .orEmpty()

    fun save(context: Context, prompt: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROMPT, prompt)
            .apply()
    }

    const val DEFAULT_PROMPT = "Correct the transcription for spelling, punctuation, and grammar while preserving the original meaning."
}
