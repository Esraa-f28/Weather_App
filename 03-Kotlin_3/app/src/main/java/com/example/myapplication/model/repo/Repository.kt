package com.example.myapplication.model.repo

import com.example.myapplication.model.local.Alert
import com.example.myapplication.model.local.FavoritePlace
import com.example.myapplication.model.local.ILocalDataSource
import com.example.myapplication.model.local.LocalDataSource
import com.example.myapplication.model.remote.IRemoteDataSource
import com.example.myapplication.model.remote.RemoteDataSource
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

class Repository(
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource
) {
    companion object {
        @Volatile
        private var instance: Repository? = null

        fun getInstance(
            remoteDataSource: IRemoteDataSource,
            localDataSource: ILocalDataSource
        ): Repository {
            return instance ?: synchronized(this) {
                instance ?: Repository(remoteDataSource, localDataSource).also {
                    instance = it
                }
            }
        }
    }

    suspend fun getHourlyForecast(
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        apiKey: String,
        units: String = "metric",
        language: String = "en",
        cityId: Int = 0
    ): Result<WeatherResponse> {
        val result = remoteDataSource.fetchHourlyForecast(
            latitude = latitude,
            longitude = longitude,
            apiKey = apiKey,
            units = units,
            language = language
        )
        if (result.isSuccess) {
            result.getOrNull()?.let { weatherResponse ->
                localDataSource.saveHourlyWeatherResponse(weatherResponse)
            }
        }
        return result
    }

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        language: String = "en"
    ): Result<CurrentWeatherResponse> {
        val remoteResult = remoteDataSource.fetchCurrentWeather(
            latitude = latitude,
            longitude = longitude,
            apiKey = apiKey,
            units = units,
            language = language
        )
        if (remoteResult.isSuccess) {
            remoteResult.getOrNull()?.let { weatherResponse ->
                localDataSource.saveCurrentWeatherResponse(weatherResponse)
            }
        }
        return remoteResult
    }

    suspend fun saveCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse) {
        localDataSource.saveCurrentWeatherResponse(weatherResponse)
    }

    suspend fun saveHourlyWeatherResponse(weatherResponse: WeatherResponse) {
        localDataSource.saveHourlyWeatherResponse(weatherResponse)
    }

    suspend fun getHourlyForecastLocal(id: Long): WeatherResponse? {
        return localDataSource.getCachedHourlyWeatherResponse(id)
    }

    suspend fun getCurrentWeatherLocal(id: Long): CurrentWeatherResponse? {
        return localDataSource.getCachedCurrentWeatherResponse(id)
    }

    suspend fun getLastWeatherId(): Long? {
        return localDataSource.getLastWeatherId()
    }

    suspend fun getFavoritePlaces(): List<FavoritePlace> {
        return localDataSource.getFavoritePlaces()
    }

    suspend fun addFavoritePlace(place: FavoritePlace) {
        localDataSource.addFavoritePlace(place)
    }

    suspend fun deleteFavoritePlace(place: FavoritePlace) {
        localDataSource.deleteFavoritePlace(place)
    }

    suspend fun getAlerts(): List<Alert> {
        return localDataSource.getAlerts()
    }

    suspend fun addAlert(alert: Alert) {
        localDataSource.addAlert(alert)
    }

    suspend fun updateAlert(alert: Alert) {
        localDataSource.updateAlert(alert)
    }

    suspend fun updateAlertStatus(alertId: String, isActive: Boolean) {
        localDataSource.updateAlertStatus(alertId, isActive)
    }

    suspend fun deleteAlert(alertId: String) {
        localDataSource.deleteAlert(alertId)
    }
}