package com.bayg.data.remote

import com.bayg.data.remote.model.WeatherResponse

interface WeatherDataSource {
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse>
}
