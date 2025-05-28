package com.example.myapplication.model.remote

import android.util.Log
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteDataSource : IRemoteDataSource {
    private val apiService: ApiService = RetrofitClient.service

    override suspend fun fetchHourlyForecast(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response: Response<WeatherResponse> = apiService.getHourlyForecast(
                latitude, longitude, apiKey, units, language
            )
            if (response.isSuccessful) {
                response.body()?.let { weatherResponse ->
                    Result.success(weatherResponse)
                } ?: run {
                    Log.e(TAG, "Hourly forecast response body is null")
                    Result.failure(Exception("Hourly forecast response body is null"))
                }
            } else {
                Log.e(TAG, "Hourly forecast API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Hourly forecast API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hourly forecast: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<CurrentWeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response: Response<CurrentWeatherResponse> = apiService.getCurrentWeather(
                latitude, longitude, apiKey, units, language
            )
            if (response.isSuccessful) {
                response.body()?.let { weatherResponse ->
                    Result.success(weatherResponse)
                } ?: run {
                    Log.e(TAG, "Current weather response body is null")
                    Result.failure(Exception("Current weather response body is null"))
                }
            } else {
                Log.e(TAG, "Current weather API call failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Current weather API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching current weather: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "RemoteDataSource"
    }
}