package com.example.myapplication.model.local

import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

interface ILocalDataSource {
    suspend fun saveCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse)
    suspend fun saveHourlyWeatherResponse(weatherResponse: WeatherResponse)
    suspend fun getCachedCurrentWeatherResponse(id: Long): CurrentWeatherResponse?
    suspend fun getCachedHourlyWeatherResponse(id: Long): WeatherResponse?
    suspend fun getLastWeatherId(): Long?
    suspend fun getFavoritePlaces(): List<FavoritePlace>
    suspend fun addFavoritePlace(place: FavoritePlace)
    suspend fun deleteFavoritePlace(place: FavoritePlace)
    suspend fun getAlerts(): List<Alert>
    suspend fun addAlert(alert: Alert)
    suspend fun updateAlert(alert: Alert)
    suspend fun updateAlertStatus(alertId: String, isActive: Boolean)
    suspend fun deleteAlert(alertId: String)
}