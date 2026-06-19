package com.bayg.data.remote.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherResponseParsingTest {

    private val gson = Gson()

    @Test
    fun `parses OpenWeather JSON response`() {
        val json = """
            {
              "weather": [{ "main": "Rain", "description": "light rain" }],
              "main": { "temp": 13.4, "feels_like": 12.1, "humidity": 68 },
              "wind": { "speed": 3.2 },
              "name": "Emmen"
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, WeatherResponse::class.java)

        assertEquals("Emmen", parsed.name)
        assertEquals("Rain", parsed.weather.first().main)
        assertEquals(13.4, parsed.main.temp, 0.001)
        assertEquals(68, parsed.main.humidity)
    }
}
