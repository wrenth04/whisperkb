package app.whisperkb.provider

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CloudProviderClient(private val config: CloudProviderConfig) {
    fun correctText(rawText: String, prompt: String): Result<String> {
        if (!config.isValid) {
            return Result.failure(IllegalStateException("Provider configuration is incomplete"))
        }

        return runCatching {
            val payload = buildRequestBody(rawText, prompt)
            val connection = (URL(config.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("Provider request failed: $responseCode ${body.ifBlank { connection.responseMessage }}")
            }
            parseResponse(body)
        }
    }

    private fun buildRequestBody(rawText: String, prompt: String): String {
        val escapedRaw = rawText.jsonEscape()
        val escapedPrompt = prompt.jsonEscape()
        val escapedModel = config.model.jsonEscape()
        return """
            {
              "model": "$escapedModel",
              "messages": [
                {"role": "system", "content": "$escapedPrompt"},
                {"role": "user", "content": "$escapedRaw"}
              ]
            }
        """.trimIndent()
    }

    private fun parseResponse(body: String): String {
        val marker = "\"content\""
        val index = body.indexOf(marker)
        if (index == -1) return body.trim()
        val start = body.indexOf(':', index).takeIf { it != -1 } ?: return body.trim()
        val firstQuote = body.indexOf('"', start + 1).takeIf { it != -1 } ?: return body.trim()
        val secondQuote = body.indexOf('"', firstQuote + 1).takeIf { it != -1 } ?: return body.trim()
        return body.substring(firstQuote + 1, secondQuote)
    }

    private fun String.jsonEscape(): String = buildString(length) {
        for (ch in this@jsonEscape) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
