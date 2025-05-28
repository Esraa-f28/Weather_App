package com.example.myapplication.model.local

import android.content.Context
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

class LocalDataSource private constructor(context: Context) : ILocalDataSource {
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

    override suspend fun saveCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse) {
        weatherDao.insertCurrentWeatherResponse(weatherResponse)
    }

    override suspend fun saveHourlyWeatherResponse(weatherResponse: WeatherResponse) {
        weatherDao.insertHourlyWeatherResponse(weatherResponse)
    }

    override suspend fun getCachedCurrentWeatherResponse(id: Long): CurrentWeatherResponse? {
        return weatherDao.getCurrentWeatherResponseById(id)
    }

    override suspend fun getCachedHourlyWeatherResponse(id: Long): WeatherResponse? {
        return weatherDao.getHourlyWeatherResponseById(id)
    }

    override suspend fun getLastWeatherId(): Long? {
        return weatherDao.getLastWeatherId()
    }

    override suspend fun getFavoritePlaces(): List<FavoritePlace> {
        return weatherDao.getFavoritePlaces()
    }

    override suspend fun addFavoritePlace(place: FavoritePlace) {
        weatherDao.insertFavoritePlace(place)
    }

    override suspend fun deleteFavoritePlace(place: FavoritePlace) {
        weatherDao.deleteFavoritePlace(place)
    }

    override suspend fun getAlerts(): List<Alert> {
        return weatherDao.getAlerts()
    }

    override suspend fun addAlert(alert: Alert) {
        weatherDao.addAlert(alert)
    }
    override suspend fun updateAlert(alert: Alert) {
        weatherDao.updateAlert(alert)
    }

    override suspend fun updateAlertStatus(alertId: String, isActive: Boolean) {
        weatherDao.updateAlertStatus(alertId, isActive)
    }

    override suspend fun deleteAlert(alertId: String) {
        weatherDao.deleteAlert(alertId)
    }
}