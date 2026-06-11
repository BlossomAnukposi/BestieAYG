package com.bayg.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object NoAsAService {
    private const val TAG = "NoAsAService"
    private const val ENDPOINT = "https://noasaservice.lol/api/get"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000
    private const val HTTP_SUCCESS_MIN = 200
    private const val HTTP_SUCCESS_MAX = 299

    suspend fun fetchMessage(): String = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(ENDPOINT)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
            }

            val code = conn.responseCode
            val stream = if (code in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }

            return@withContext sanitizeMessage(extractMessage((body)))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching message", e)
            return@withContext "Unexpected error: ${e.message ?: "unknown"}"
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) { /* ignore */ }
        }
    }

    private fun extractMessage(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) return body

        return try {
            val json = JSONObject(trimmed)
            val message = json.optString("message", "").trim()
            message.ifEmpty { body }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse JSON message: ${e.message}")
            body
        }
    }

    private fun sanitizeMessage(input: String): String {
        // 1. Hard length cap – prevents UI flooding / memory abuse
        val truncated = if (input.length > 500) input.substring(0, 500) + "…" else input

        // 2. Strip all HTML/XML tags  (e.g. <script>alert(1)</script>)
        val noHtml = truncated.replace(Regex("<[^>]*>"), "")

        // 3. Remove JavaScript URI schemes  (e.g. javascript:alert(1))
        val noJsUri = noHtml.replace(Regex("(?i)javascript\\s*:"), "")

        // 4. Strip null bytes and other non-printable control characters
        //    (keeps normal whitespace: \t, \n, \r)
        val noControl = noJsUri.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")

        // 5. Collapse any run of whitespace/newlines to a single space
        //    (prevents invisible Unicode padding / layout attacks)
        val normalized = noControl.replace(Regex("[\\s]+"), " ").trim()

        return normalized
    }
}