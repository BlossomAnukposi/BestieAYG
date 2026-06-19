package com.bayg.ui.viewmodel

import com.bayg.data.remote.WeatherDataSource
import com.bayg.data.remote.model.MainData
import com.bayg.data.remote.model.WeatherDescription
import com.bayg.data.remote.model.WeatherResponse
import com.bayg.data.remote.model.WindData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchWeather emits success when repository succeeds`() = runTest(testDispatcher) {
        val weather = sampleWeather()
        val viewModel = WeatherViewModel(
            repository = FakeWeatherDataSource(Result.success(weather)),
        )

        viewModel.fetchWeather(52.78, 6.90)
        advanceUntilIdle()

        val state = viewModel.weatherState.value
        assertTrue(state is WeatherUiState.Success)
        assertEquals("Emmen", (state as WeatherUiState.Success).weather.name)
    }

    @Test
    fun `fetchWeather emits error when repository fails`() = runTest(testDispatcher) {
        val viewModel = WeatherViewModel(
            repository = FakeWeatherDataSource(Result.failure(IllegalStateException("Network down"))),
        )

        viewModel.fetchWeather(52.78, 6.90)
        advanceUntilIdle()

        val state = viewModel.weatherState.value
        assertTrue(state is WeatherUiState.Error)
        assertEquals("Network down", (state as WeatherUiState.Error).message)
    }

    private fun sampleWeather() = WeatherResponse(
        weather = listOf(WeatherDescription(main = "Rain", description = "light rain")),
        main = MainData(temp = 13.0, feelsLike = 12.0, humidity = 68),
        wind = WindData(speed = 3.0),
        name = "Emmen",
    )

    private class FakeWeatherDataSource(
        private val result: Result<WeatherResponse>,
    ) : WeatherDataSource {
        override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherResponse> = result
    }
}
