package com.bayg.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayg.BuildConfig
import com.bayg.data.remote.WeatherDataSource
import com.bayg.data.remote.WeatherRepository
import com.bayg.data.remote.model.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val weather: WeatherResponse) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(
    private val repository: WeatherDataSource = WeatherRepository(),
    private val hasApiKey: () -> Boolean = { BuildConfig.OPENWEATHER_API_KEY.isNotBlank() },
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            if (!hasApiKey()) {
                _weatherState.value = WeatherUiState.Error(
                    "Missing OPENWEATHER_API_KEY in local.properties"
                )
                return@launch
            }

            _weatherState.value = WeatherUiState.Loading

            repository.getWeather(latitude, longitude)
                .onSuccess { weather ->
                    _weatherState.value = WeatherUiState.Success(weather)
                }
                .onFailure { error ->
                    _weatherState.value = WeatherUiState.Error(
                        error.message ?: "Unknown error"
                    )
                }
        }
    }
}
