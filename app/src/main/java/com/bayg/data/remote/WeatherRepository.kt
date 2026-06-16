package com.bayg.data.remote

import com.bayg.BuildConfig
import com.bayg.data.remote.model.WeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * WeatherRepository — handles all weather requests for the app.
 *
 * Architecture:
 *   App ─► BestieAYG Cloudflare Worker proxy ─► OpenWeather
 *
 * Security notes:
 * - The OpenWeather API key is NOT in the APK. It lives only in the
 *   Cloudflare Worker's encrypted secret store (proxy/src/index.js).
 * - The proxy URL itself is not a secret; certificate validation is
 *   handled by the system trust store + the platform's TLS stack.
 * - Network logging is disabled in release builds so coordinates never
 *   leak into logcat.
 * - Returns Result<T> so callers handle errors gracefully.
 */
class WeatherRepository : WeatherDataSource {

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
        .baseUrl(PROXY_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(WeatherApiService::class.java)

    override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val response = api.getCurrentWeather(
                latitude = latitude,
                longitude = longitude
            )
            Result.success(response)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }

    companion object {
        // Public proxy endpoint. Not a secret — the secret OpenWeather key
        // lives only inside the Worker (Cloudflare encrypted env).
        private const val PROXY_BASE_URL =
            "https://bayg-weather-proxy.bayg-weather-proxy.workers.dev/"
    }
}
