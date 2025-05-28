package com.example.myapplication.model.remote

import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import com.example.weatherapp.model.pojos.WeatherResponse
import io.mockk.coEvery
import io.mockk.mockk

class FakeRemoteDataSource(
    private val weatherResponses: MutableMap<Pair<Double, Double>, WeatherResponse> = mutableMapOf(),
    private val currentWeatherResponses: MutableMap<Pair<Double, Double>, CurrentWeatherResponse> = mutableMapOf(),
) : IRemoteDataSource {

    // Create a mock instance for IRemoteDataSource
    private val remoteDataSourceMock: IRemoteDataSource = mockk<IRemoteDataSource>().apply {
        coEvery {
            fetchHourlyForecast(
                latitude = any(),
                longitude = any(),
                apiKey = any(),
                units = any(),
                language = any()
            )
        } answers {
            val latitude = firstArg<Double>()
            val longitude = secondArg<Double>()
            weatherResponses[Pair(latitude, longitude)]?.let { Result.success<WeatherResponse>(it) }
                ?: Result.failure(Exception("No hourly forecast for coordinates ($latitude, $longitude)"))
        }

        coEvery {
            fetchCurrentWeather(
                latitude = any(),
                longitude = any(),
                apiKey = any(),
                units = any(),
                language = any()
            )
        } answers {
            val latitude = firstArg<Double>()
            val longitude = secondArg<Double>()
            currentWeatherResponses[Pair(latitude, longitude)]?.let { Result.success<CurrentWeatherResponse>(it) }
                ?: Result.failure(Exception("No current weather for coordinates ($latitude, $longitude)"))
        }
    }

    override suspend fun fetchHourlyForecast(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<WeatherResponse> {
        return remoteDataSourceMock.fetchHourlyForecast(latitude, longitude, apiKey, units, language)
    }

    override suspend fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String,
        language: String
    ): Result<CurrentWeatherResponse> {
        return remoteDataSourceMock.fetchCurrentWeather(latitude, longitude, apiKey, units, language)
    }
}