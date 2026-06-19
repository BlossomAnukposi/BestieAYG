package com.bayg.services

import com.google.gson.JsonParser

/**
 * Parses and sanitizes quote text from the No-As-A-Service API before it
 * reaches the UI. Kept separate so the rules can be unit-tested.
 */
object NoAsAMessageSanitizer {

    fun extractMessage(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) return body

        return try {
            val json = JsonParser.parseString(trimmed).asJsonObject
            val message = json.get("message")?.asString?.trim().orEmpty()
            message.ifEmpty { body }
        } catch (_: Exception) {
            body
        }
    }

    fun sanitizeMessage(input: String): String {
        val truncated = if (input.length > 500) input.substring(0, 500) + "…" else input
        val noScript = truncated.replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
        val noHtml = noScript.replace(Regex("<[^>]*>"), "")
        val noJsUri = noHtml.replace(Regex("(?i)javascript\\s*:"), "")
        val noControl = noJsUri.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
        return noControl.replace(Regex("[\\s]+"), " ").trim()
    }
}
