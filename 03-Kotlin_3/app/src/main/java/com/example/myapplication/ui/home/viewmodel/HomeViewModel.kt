package com.example.myapplication.ui.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.repo.Repository
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherData
import kotlinx.coroutines.Dispatchers
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
        if (_tempUnit.value != tempUnit) _tempUnit.value = tempUnit
        if (_windUnit.value != windUnit) _windUnit.value = windUnit
        if (_language.value != language) _language.value = language
    }

    fun fetchWeatherData(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en",
        isNetworkAvailable: Boolean
    ) {
        if (!isNetworkAvailable) {
            getCachedWeatherData(latitude, longitude, tempUnit.value ?: "celsius", windUnit.value ?: "m/s")
            return
        }

        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentWeatherResult = repository.getCurrentWeather(latitude, longitude, apiKey, units, lang)
                if (currentWeatherResult.isSuccess) {
                    currentWeatherResult.getOrNull()?.let { currentWeather ->
                        if (_currentWeather.value != currentWeather) {
                            _currentWeather.postValue(currentWeather)
                            repository.saveCurrentWeatherResponse(currentWeather)
                        }
                    }
                } else {
                    _error.postValue(currentWeatherResult.exceptionOrNull()?.localizedMessage ?: "Unknown error fetching current weather")
                }

                val forecastResult = repository.getHourlyForecast(latitude, longitude, apiKey, units, lang)
                if (forecastResult.isSuccess) {
                    forecastResult.getOrNull()?.let { forecastResponse ->
                        val hourlyData = forecastResponse.list.take(8)
                        if (_hourlyForecast.value != hourlyData) {
                            _hourlyForecast.postValue(hourlyData)
                        }

                        val dailyData = processDailyForecast(forecastResponse.list)
                        if (_dailyForecast.value != dailyData) {
                            _dailyForecast.postValue(dailyData)
                        }

                        repository.saveHourlyWeatherResponse(forecastResponse)
                    } ?: run {
                        _error.postValue("Forecast data is null")
                    }
                } else {
                    _error.postValue(forecastResult.exceptionOrNull()?.localizedMessage ?: "Unknown error fetching forecast")
                }
            } catch (e: Exception) {
                _error.postValue("Failed to fetch weather data: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun getCachedWeatherData(latitude: Double, longitude: Double, tempUnit: String, windUnit: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val lastWeatherId = repository.getLastWeatherId()
                if (lastWeatherId != null) {
                    val cachedCurrent = repository.getCurrentWeatherLocal(lastWeatherId)
                    if (cachedCurrent != null) {
                        val convertedCurrent = convertCurrentWeather(cachedCurrent, tempUnit, windUnit)
                        if (_currentWeather.value != convertedCurrent) {
                            _currentWeather.postValue(convertedCurrent)
                        }
                    } else {
                        _error.postValue("No cached current weather data available for ID: $lastWeatherId")
                    }

                    val cachedForecast = repository.getHourlyForecastLocal(lastWeatherId)
                    if (cachedForecast != null) {
                        val hourlyData = cachedForecast.list.take(8).map { convertWeatherData(it, tempUnit) }
                        if (_hourlyForecast.value != hourlyData) {
                            _hourlyForecast.postValue(hourlyData)
                        }

                        val dailyData = processDailyForecast(cachedForecast.list).map { convertWeatherData(it, tempUnit) }
                        if (_dailyForecast.value != dailyData) {
                            _dailyForecast.postValue(dailyData)
                        }
                    } else {
                        _error.postValue("No cached forecast data available for ID: $lastWeatherId")
                    }
                } else {
                    _error.postValue("No cached weather data available")
                }
            } catch (e: Exception) {
                _error.postValue("Failed to retrieve cached weather data: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
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
            .mapNotNull { (date, dailyForecasts) ->
                // Select the forecast closest to noon for display
                val representativeForecast = dailyForecasts.minByOrNull { forecast ->
                    val time = forecast.dtTxt.substring(11, 16)
                    val hours = time.substring(0, 2).toInt()
                    kotlin.math.abs(hours - 12)
                } ?: return@mapNotNull null

                // Calculate actual max and min temperatures for the day
                val temps = dailyForecasts.map { it.main.temp }
                val tempMax = temps.maxOrNull() ?: representativeForecast.main.temp
                val tempMin = temps.minOrNull() ?: representativeForecast.main.temp

                representativeForecast.copy(
                    main = representativeForecast.main.copy(
                        tempMax = tempMax,
                        tempMin = tempMin
                    )
                )
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
        val tempMin = when (tempUnit) {
            "fahrenheit" -> (data.main.tempMin * 9 / 5) + 32
            "kelvin" -> data.main.tempMin + 273.15f
            else -> data.main.tempMin
        }
        return data.copy(main = data.main.copy(temp = temp, tempMax = tempMax, tempMin = tempMin))
    }

    fun clearError() {
        _error.value = ""
    }
}