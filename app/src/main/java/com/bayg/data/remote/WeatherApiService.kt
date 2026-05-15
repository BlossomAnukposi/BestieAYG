package com.bayg.data.remote

import com.bayg.data.remote.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    /**
     * Fetches current weather for a given latitude and longitude.
     * OpenWeather endpoint: GET /data/2.5/weather
     *
     * @param latitude  GPS latitude of the user
     * @param longitude GPS longitude of the user
     * @param units     "metric" returns °C and m/s
     * @param apiKey    OpenWeather API key (injected from BuildConfig — never hardcoded)
     */
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): WeatherResponse
}
