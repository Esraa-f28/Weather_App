package com.example.myapplication.model.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeatherResponse(weatherResponse: CurrentWeatherResponse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyWeatherResponse(weatherResponse: WeatherResponse)

    @Query("SELECT * FROM current_weather_response WHERE id_current = :id ORDER BY id_current DESC LIMIT 1")
    suspend fun getCurrentWeatherResponseById(id: Long): CurrentWeatherResponse?

    @Query("SELECT * FROM weather_response WHERE id = :id ORDER BY id DESC LIMIT 1")
    suspend fun getHourlyWeatherResponseById(id: Long): WeatherResponse?
    @Query("SELECT MAX(id_current) FROM current_weather_response")
    suspend fun getLastWeatherId(): Long?

    @Query("SELECT * FROM favorite_places")
    suspend fun getFavoritePlaces(): List<FavoritePlace>

    @Insert
    suspend fun insertFavoritePlace(place: FavoritePlace)

    @Delete
    suspend fun deleteFavoritePlace(place: FavoritePlace)

    @Query("SELECT * FROM alert")
    suspend fun getAlerts(): List<Alert>
    @Update
    suspend fun updateAlert(alert: Alert)
    @Insert
    suspend fun addAlert(alert: Alert)

    @Query("UPDATE alert SET isActive = :isActive WHERE id = :alertId")
    suspend fun updateAlertStatus(alertId: String, isActive: Boolean)

    @Query("DELETE FROM alert WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)
}