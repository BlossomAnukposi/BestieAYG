package com.bayg.data.remote

import com.bayg.data.remote.model.CbsDataPoint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class CbsRepository {

    companion object {
        // Average daily social media usage in the Netherlands in minutes.
        // Source: Newcom National Social Media Research 2024
        const val NL_AVERAGE_SOCIAL_MEDIA_MINUTES = 138
    }

    private val httpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://opendata.cbs.nl/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CbsApiService::class.java)

    suspend fun getSocialNetworkUsage(): Result<CbsDataPoint> {
        return try {
            val response = api.getSocialNetworkStats()
            val dataPoint = response.value.firstOrNull()
                ?: return Result.failure(Exception("No CBS data available"))
            Result.success(dataPoint)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    // Formats a combined comparison string using:
    // - userMinutes: the user's actual daily Instagram usage (from AppUsageManager)
    // - socialNetworkPercent: live % of Dutch people on social networks (from CBS OData API)
    // - NL_AVERAGE_SOCIAL_MEDIA_MINUTES: hardcoded NL daily average (Newcom 2024)
    // Example output: "You: 2h 45min vs NL Average: 2h 18min, 79.5% of Dutch people use social networks"
    fun formatComparison(userMinutes: Int, socialNetworkPercent: Double? = null): String {
        val userHours = userMinutes / 60
        val userMins = userMinutes % 60
        val nlHours = NL_AVERAGE_SOCIAL_MEDIA_MINUTES / 60
        val nlMins = NL_AVERAGE_SOCIAL_MEDIA_MINUTES % 60
        val timeComparison = "You: ${userHours}h ${userMins}min vs NL Average: ${nlHours}h ${nlMins}min"
        val percentPart = if (socialNetworkPercent != null) {
            ", ${socialNetworkPercent}% of Dutch people use social networks"
        } else {
            ""
        }
        return "$timeComparison$percentPart"
    }
}
