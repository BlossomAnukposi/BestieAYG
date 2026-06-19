package com.bayg.data.remote

import com.bayg.data.remote.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    /**
     * Fetches current weather for a given latitude and longitude from the
     * BestieAYG weather proxy (Cloudflare Worker).
     *
     * Endpoint: GET https://<proxy>/weather?lat=..&lon=..
     *
     * The proxy holds the OpenWeather API key server-side and forwards the
     * request to OpenWeather, so no API key is ever shipped in the APK.
     * See `proxy/src/index.js` and the threat-model entry "OpenWeather API
     * key extracted from APK" (now Mitigated).
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): WeatherResponse
}
