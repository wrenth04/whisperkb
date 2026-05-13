package app.whisperkb.provider

data class CloudProviderConfig(
    val name: String,
    val endpoint: String,
    val apiKey: String,
    val model: String,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}
