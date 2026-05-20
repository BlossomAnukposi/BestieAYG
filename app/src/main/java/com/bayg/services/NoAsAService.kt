package com.bayg.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object NoAsAService {
    private const val TAG = "NoAsAService"
    private const val ENDPOINT = "https://noasaservice.lol/api/get"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    /**
     * Fetches the message from the no-as-a-service GET endpoint.
     * Returns the raw response body as a String, or an error text.
     */
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
            body
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching message", e)
            "Network error: ${e.message ?: "timeout"}"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching message", e)
            "Unexpected error: ${e.message ?: "unknown"}"
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) { /* ignore */ }
        }
    }
}