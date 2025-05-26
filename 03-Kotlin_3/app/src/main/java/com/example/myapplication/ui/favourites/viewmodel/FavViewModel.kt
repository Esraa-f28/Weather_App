package com.example.myapplication.ui.favourites.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.local.FavoritePlace
import com.example.myapplication.model.repo.Repository
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherData
import kotlinx.coroutines.launch

class FavViewModel(private val repository: Repository) : ViewModel() {

    private val _favoritePlaces = MutableLiveData<List<FavoritePlace>>()
    val favoritePlaces: LiveData<List<FavoritePlace>> = _favoritePlaces

    private val _selectedPlace = MutableLiveData<FavoritePlace?>()
    val selectedPlace: LiveData<FavoritePlace?> = _selectedPlace

    // Current weather data
    private val _currentWeather = MutableLiveData<CurrentWeatherResponse>()
    val currentWeather: LiveData<CurrentWeatherResponse> get() = _currentWeather

    // Hourly forecast data
    private val _hourlyForecast = MutableLiveData<List<WeatherData>>()
    val hourlyForecast: LiveData<List<WeatherData>> get() = _hourlyForecast

    // Daily forecast data
    private val _dailyForecast = MutableLiveData<List<WeatherData>>()
    val dailyForecast: LiveData<List<WeatherData>> get() = _dailyForecast

    private val _tempUnit = MutableLiveData<String>()
    val tempUnit: LiveData<String> get() = _tempUnit

    private val _language = MutableLiveData<String>()
    val language: LiveData<String> get() = _language

    private val _windUnit = MutableLiveData<String>()
    val windUnit: LiveData<String> get() = _windUnit

    init {
        loadFavoritePlaces()
    }

    private fun loadFavoritePlaces() {
        viewModelScope.launch {
            _favoritePlaces.value = repository.getFavoritePlaces()
        }
    }

    fun addFavoritePlace(latitude: Double, longitude: Double, name: String, cityname: String) {
        viewModelScope.launch {
            val place = FavoritePlace(
                latitude = latitude,
                longitude = longitude,
                name = name,
                city = cityname
            )
            repository.addFavoritePlace(place)
            loadFavoritePlaces()
        }
    }

    fun deleteFavoritePlace(place: FavoritePlace) {
        viewModelScope.launch {
            repository.deleteFavoritePlace(place)
            loadFavoritePlaces()
        }
    }

    fun setSelectedPlace(place: FavoritePlace?) {
        _selectedPlace.value = place
    }

    fun fetchWeatherData(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        lang: String = "en",
    ) {

        viewModelScope.launch {
                // Fetch from remote API
                val currentWeatherResult =
                    repository.getCurrentWeather(latitude, longitude, apiKey, units, lang)
                if (currentWeatherResult.isSuccess) {
                    currentWeatherResult.getOrNull()?.let { _currentWeather.value = it }
                }
                val forecastResult =
                    repository.getHourlyForecast(latitude, longitude, apiKey, units, lang)
                if (forecastResult.isSuccess) {
                    forecastResult.getOrNull()?.let { forecastResponse ->
                        val hourlyData = forecastResponse.list.take(8)
                        _hourlyForecast.value = hourlyData
                        // Process daily forecast (group by day)
                        val dailyData = processDailyForecast(forecastResponse.list)
                        _dailyForecast.value = dailyData
                    } ?: run {
                    }
                }

        }
    }

    private fun processDailyForecast(forecastList: List<WeatherData>): List<WeatherData> {
        // Group by day and get one entry per day (closest to midday, 12:00)
        return forecastList
            .groupBy { it.dtTxt.substring(0, 10) } // Group by date (yyyy-MM-dd)
            .mapNotNull { (_, dailyForecasts) ->
                // Find the forecast closest to midday (12:00)
                dailyForecasts.minByOrNull { forecast ->
                    val time = forecast.dtTxt.substring(11, 16) // Extract HH:mm
                    val hours = time.substring(0, 2).toInt()
                    kotlin.math.abs(hours - 12)
                }
            }
            .take(4) // Take next 4 days
    }
}