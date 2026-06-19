package com.bayg.data.remote

import com.bayg.BuildConfig
import com.bayg.data.remote.model.WeatherResponse
import okhttp3.Interceptor
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

    /**
     * Tags every request to the proxy with X-Bayg-Client so the Worker
     * can reject random scrapers that have discovered the public URL.
     * This is not authentication (an attacker who decompiles the APK
     * sees the header); quota protection comes from the Worker's
     * per-IP rate limit.
     */
    private val clientHeaderInterceptor = Interceptor { chain ->
        val tagged = chain.request().newBuilder()
            .header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            .build()
        chain.proceed(tagged)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(clientHeaderInterceptor)
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        // Public proxy endpoint. Not a secret — the secret OpenWeather key
        // lives only inside the Worker (Cloudflare encrypted env).
        private const val PROXY_BASE_URL =
            "https://bayg-weather-proxy.bayg-weather-proxy.workers.dev/"

        // Sent on every /weather request; matches REQUIRED_CLIENT_VALUE
        // in proxy/src/index.js.
        private const val CLIENT_HEADER_NAME = "X-Bayg-Client"
        private const val CLIENT_HEADER_VALUE = "bayg-android"
    }
}
