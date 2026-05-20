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
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }

            return@withContext extractMessage(body)
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
}