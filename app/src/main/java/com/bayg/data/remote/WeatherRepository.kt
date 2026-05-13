package com.bayg.data.remote

import com.bayg.BuildConfig
import com.bayg.data.remote.model.WeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * WeatherRepository — handles all communication with the OpenWeather API.
 *
 * Security notes:
 * - API key is read from BuildConfig (injected at build time from local.properties)
 * - Network logging is disabled in release builds to prevent key/coord leakage in logs
 * - Returns Result<T> so callers handle errors gracefully (no crashes on network failure)
 */
class WeatherRepository {

    private val apiKey = BuildConfig.OPENWEATHER_API_KEY

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(WeatherApiService::class.java)

    /**
     * Fetches current weather for the given coordinates.
     *
     * @param latitude  User's GPS latitude
     * @param longitude User's GPS longitude
     * @return Result.success(WeatherResponse) on success
     *         Result.failure(Exception) on network error or bad response
     *
     * Usage in ViewModel:
     *   val result = weatherRepository.getWeather(lat, lon)
     *   result.onSuccess { weather -> ... }
     *   result.onFailure { error -> ... }
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val response = api.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = apiKey
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
