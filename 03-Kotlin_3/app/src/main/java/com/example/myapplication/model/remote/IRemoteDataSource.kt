package com.example.myapplication.model.remote

import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse

interface IRemoteDataSource {
    suspend fun fetchHourlyForecast(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<WeatherResponse>

    suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<CurrentWeatherResponse>
}