package com.bayg.data.remote.model

data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: MainData,
    val wind: WindData,
    val name: String
)

data class WeatherDescription(
    val main: String,
    val description: String
)

data class MainData(
    val temp: Double,
    val feels_like: Double,
    val humidity: Int
)

data class WindData(
    val speed: Double
)
