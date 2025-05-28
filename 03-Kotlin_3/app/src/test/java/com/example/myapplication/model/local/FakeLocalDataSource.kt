package com.example.myapplication.model.local

import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

class FakeLocalDataSource(
    private val weatherResponses: MutableMap<Pair<Double, Double>, WeatherResponse> = mutableMapOf(),
    private val currentWeatherResponses: MutableMap<Pair<Double, Double>, CurrentWeatherResponse> = mutableMapOf(),
    private val favoritePlaces: MutableList<FavoritePlace> = mutableListOf(),
    private val alerts: MutableList<Alert> = mutableListOf(),
) : ILocalDataSource {

    override suspend fun saveCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse) {
        currentWeatherResponses.clear() // Only one current weather response stored at a time
        currentWeatherResponses[Pair(weatherResponse.coord.lat, weatherResponse.coord.lon)] = weatherResponse
    }

    override suspend fun saveHourlyWeatherResponse(weatherResponse: WeatherResponse) {
        weatherResponses.clear() // Only one weather response stored at a time
        weatherResponses[Pair(weatherResponse.city.coord.lat.toDouble(), weatherResponse.city.coord.lon.toDouble())] = weatherResponse
    }

    override suspend fun getCachedCurrentWeatherResponse(id: Long): CurrentWeatherResponse? {
        return currentWeatherResponses.values.firstOrNull()
    }

    override suspend fun getCachedHourlyWeatherResponse(id: Long): WeatherResponse? {
        return weatherResponses.values.firstOrNull()
    }

    override suspend fun getLastWeatherId(): Long? {
        return if (weatherResponses.isNotEmpty() || currentWeatherResponses.isNotEmpty()) 1L else null
    }

    override suspend fun getFavoritePlaces(): List<FavoritePlace> {
        return favoritePlaces.toList()
    }

    override suspend fun addFavoritePlace(place: FavoritePlace) {
        favoritePlaces.add(place)
    }

    override suspend fun deleteFavoritePlace(place: FavoritePlace) {
        favoritePlaces.removeIf {
            it.name == place.name && it.latitude == place.latitude && it.longitude == place.longitude
        }
    }

    override suspend fun getAlerts(): List<Alert> {
        return alerts.toList()
    }

    override suspend fun addAlert(alert: Alert) {
        val id = (alerts.size + 1).toString()
        val alertWithId = alert.copy(id = id)
        alerts.add(alertWithId)
    }

    override suspend fun updateAlertStatus(alertId: String, isActive: Boolean) {
        alerts.find { it.id == alertId }?.let { alert ->
            val updatedAlert = alert.copy(isActive = isActive)
            alerts[alerts.indexOf(alert)] = updatedAlert
        }
    }

    override suspend fun deleteAlert(alertId: String) {
        alerts.removeIf { it.id == alertId }
    }
    override suspend fun updateAlert(alert: Alert){

    }
}