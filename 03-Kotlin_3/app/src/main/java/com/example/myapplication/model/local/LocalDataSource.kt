package com.example.myapplication.model.local

import android.content.Context
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

class LocalDataSource private constructor(context: Context) {

    private val weatherDao: WeatherDao

    companion object {
        @Volatile
        private var instance: LocalDataSource? = null

        fun getInstance(context: Context): LocalDataSource {
            return instance ?: synchronized(this) {
                instance ?: LocalDataSource(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        val database = AppDatabase.getDatabase(context)
        weatherDao = database.weatherDao()
    }

    suspend fun saveCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse) {
        weatherDao.insertCurrentWeatherResponse(weatherResponse)
    }

    suspend fun saveHourlyWeatherResponse(weatherResponse: WeatherResponse) {
        weatherDao.insertHourlyWeatherResponse(weatherResponse)
    }

    suspend fun getCachedCurrentWeatherResponse(id: Long): CurrentWeatherResponse? {
        return weatherDao.getCurrentWeatherResponseById(id)
    }

    suspend fun getCachedHourlyWeatherResponse(id: Long): WeatherResponse? {
        return weatherDao.getHourlyWeatherResponseById(id)
    }

    suspend fun getLastWeatherId(): Long? {
        return weatherDao.getLastWeatherId()
    }

    suspend fun getFavoritePlaces(): List<FavoritePlace> {
        return weatherDao.getFavoritePlaces()
    }

    suspend fun addFavoritePlace(place: FavoritePlace) {
        weatherDao.insertFavoritePlace(place)
    }

    suspend fun deleteFavoritePlace(place: FavoritePlace) {
        weatherDao.deleteFavoritePlace(place)
    }

    suspend fun getAlerts(): List<Alert> {
        return weatherDao.getAlerts()
    }

    suspend fun addAlert(alert: Alert) {
        weatherDao.addAlert(alert)
    }

    suspend fun updateAlertStatus(alertId: String, isActive: Boolean) {
        weatherDao.updateAlertStatus(alertId, isActive)
    }

    suspend fun deleteAlert(alertId: String) {
        weatherDao.deleteAlert(alertId)
    }
}