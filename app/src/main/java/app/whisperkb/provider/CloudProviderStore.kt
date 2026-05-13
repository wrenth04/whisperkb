package app.whisperkb.provider

import android.content.Context

private const val PREFS_NAME = "whisperkb_provider"
private const val KEY_NAME = "name"
private const val KEY_ENDPOINT = "endpoint"
private const val KEY_API_KEY = "api_key"
private const val KEY_MODEL = "model"

object CloudProviderStore {
    fun load(context: Context): CloudProviderConfig? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_NAME, null).orEmpty()
        val endpoint = prefs.getString(KEY_ENDPOINT, null).orEmpty()
        val apiKey = prefs.getString(KEY_API_KEY, null).orEmpty()
        val model = prefs.getString(KEY_MODEL, null).orEmpty()
        return CloudProviderConfig(name, endpoint, apiKey, model).takeIf { it.isValid }
    }

    fun save(context: Context, config: CloudProviderConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, config.name)
            .putString(KEY_ENDPOINT, config.endpoint)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .apply()
    }
}
