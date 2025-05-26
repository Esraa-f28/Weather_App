package com.example.myapplication.ui.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.repo.Repository
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherData
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(private val repository: Repository) : ViewModel() {

    private val _currentWeather = MutableLiveData<CurrentWeatherResponse>()
    val currentWeather: LiveData<CurrentWeatherResponse> get() = _currentWeather

    private val _hourlyForecast = MutableLiveData<List<WeatherData>>()
    val hourlyForecast: LiveData<List<WeatherData>> get() = _hourlyForecast

    private val _dailyForecast = MutableLiveData<List<WeatherData>>()
    val dailyForecast: LiveData<List<WeatherData>> get() = _dailyForecast

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _tempUnit = MutableLiveData<String>()
    val tempUnit: LiveData<String> get() = _tempUnit

    private val _language = MutableLiveData<String>()
    val language: LiveData<String> get() = _language

    private val _windUnit = MutableLiveData<String>()
    val windUnit: LiveData<String> get() = _windUnit

    fun setUnits(tempUnit: String, windUnit: String, language: String) {
        _tempUnit.value = tempUnit
        _windUnit.value = windUnit
        _language.value = language
    }

    fun fetchWeatherData(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en",
        isNetworkAvailable: Boolean
    ) {
        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                if (isNetworkAvailable) {
                    // Online mode: Fetch from API
                    val currentWeatherResult = repository.getCurrentWeather(latitude, longitude, apiKey, units, lang)
                    if (currentWeatherResult.isSuccess) {
                        currentWeatherResult.getOrNull()?.let { currentWeather ->
                            _currentWeather.value = currentWeather
                            repository.saveCurrentWeatherResponse(currentWeather)
                        }
                    } else {
                        _error.value = currentWeatherResult.exceptionOrNull()?.localizedMessage ?: "Unknown error fetching current weather"
                    }

                    val forecastResult = repository.getHourlyForecast(latitude, longitude, apiKey, units, lang)
                    if (forecastResult.isSuccess) {
                        forecastResult.getOrNull()?.let { forecastResponse ->
                            val hourlyData = forecastResponse.list.take(8)
                            _hourlyForecast.value = hourlyData

                            val dailyData = processDailyForecast(forecastResponse.list)
                            _dailyForecast.value = dailyData

                            repository.saveHourlyWeatherResponse(forecastResponse)
                        } ?: run {
                            _error.value = "Forecast data is null"
                        }
                    } else {
                        _error.value = forecastResult.exceptionOrNull()?.localizedMessage ?: "Unknown error fetching forecast"
                    }
                } else {
                    // Offline mode: Retrieve cached data
                    getCachedWeatherData(latitude, longitude, tempUnit.value ?: "celsius", windUnit.value ?: "m/s")
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch weather data: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCachedWeatherData(latitude: Double, longitude: Double, tempUnit: String, windUnit: String) {
        viewModelScope.launch {
            try {
                // Get the latest weather ID
                val lastWeatherId = repository.getLastWeatherId()
                if (lastWeatherId != null) {
                    // Fetch cached current weather
                    val cachedCurrent = repository.getCurrentWeatherLocal(lastWeatherId)
                    if (cachedCurrent != null) {
                        _currentWeather.value = convertCurrentWeather(cachedCurrent, tempUnit, windUnit)
                    } else {
                        _error.value = "No cached current weather data available"
                    }

                    // Fetch cached hourly forecast
                    val cachedForecast = repository.getHourlyForecastLocal(lastWeatherId)
                    if (cachedForecast != null) {
                        val hourlyData = cachedForecast.list.take(8).map { convertWeatherData(it, tempUnit) }
                        _hourlyForecast.value = hourlyData

                        val dailyData = processDailyForecast(cachedForecast.list).map { convertWeatherData(it, tempUnit) }
                        _dailyForecast.value = dailyData
                    } else {
                        _error.value = "No cached forecast data available"
                    }
                } else {
                    _error.value = "No cached weather data available"
                }
            } catch (e: Exception) {
                _error.value = "Failed to retrieve cached weather data: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun processDailyForecast(forecastList: List<WeatherData>): List<WeatherData> {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000

        return forecastList
            .filter { it.dt >= tomorrow }
            .groupBy { it.dtTxt.substring(0, 10) }
            .mapNotNull { (_, dailyForecasts) ->
                dailyForecasts.minByOrNull { forecast ->
                    val time = forecast.dtTxt.substring(11, 16)
                    val hours = time.substring(0, 2).toInt()
                    kotlin.math.abs(hours - 12)
                }
            }
            .map {
                // Set min and max temperatures to be the same
                it.copy(main = it.main.copy(tempMin = it.main.tempMax, tempMax = it.main.tempMax))
            }
            .take(4)
    }

    private fun convertCurrentWeather(weather: CurrentWeatherResponse, tempUnit: String, windUnit: String): CurrentWeatherResponse {
        val temp = when (tempUnit) {
            "fahrenheit" -> (weather.main.temp * 9 / 5) + 32
            "kelvin" -> weather.main.temp + 273.15f
            else -> weather.main.temp
        }
        val tempMin = when (tempUnit) {
            "fahrenheit" -> (weather.main.tempMin * 9 / 5) + 32
            "kelvin" -> weather.main.tempMin + 273.15f
            else -> weather.main.tempMin
        }
        val tempMax = when (tempUnit) {
            "fahrenheit" -> (weather.main.tempMax * 9 / 5) + 32
            "kelvin" -> weather.main.tempMax + 273.15f
            else -> weather.main.tempMax
        }
        val windSpeed = if (windUnit == "mph") {
            weather.wind.speed * 2.23694f
        } else {
            weather.wind.speed
        }
        return weather.copy(
            main = weather.main.copy(temp = temp, tempMin = tempMin, tempMax = tempMax),
            wind = weather.wind.copy(speed = windSpeed)
        )
    }

    private fun convertWeatherData(data: WeatherData, tempUnit: String): WeatherData {
        val temp = when (tempUnit) {
            "fahrenheit" -> (data.main.temp * 9 / 5) + 32
            "kelvin" -> data.main.temp + 273.15f
            else -> data.main.temp
        }
        val tempMax = when (tempUnit) {
            "fahrenheit" -> (data.main.tempMax * 9 / 5) + 32
            "kelvin" -> data.main.tempMax + 273.15f
            else -> data.main.tempMax
        }
        return data.copy(main = data.main.copy(temp = temp, tempMax = tempMax, tempMin = tempMax))
    }

    fun clearError() {
        _error.value = ""
    }
}