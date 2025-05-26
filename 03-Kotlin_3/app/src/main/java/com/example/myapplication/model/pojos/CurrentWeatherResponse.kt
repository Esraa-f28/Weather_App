package com.example.weatherapp.model.pojos

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapplication.model.local.Converters
import com.google.gson.annotations.SerializedName
 //name -day-date-description-icon-temp-pressure-humi-wind-cloud-visibilty-ultra violet
@Entity(tableName = "current_weather_response")
@TypeConverters(Converters::class)
data class CurrentWeatherResponse(
    @PrimaryKey(autoGenerate = true) val id_current: Long = 0,
    @SerializedName("coord") val coord: CurrentCoord,
    @SerializedName("weather") val weather: List<CurrentWeather>,
    @SerializedName("base") val base: String,
    @SerializedName("main") val main: CurrentMain,
    @SerializedName("visibility") val visibility: Long,
    @SerializedName("wind") val wind: CurrentWind,
    @SerializedName("clouds") val clouds: CurrentClouds,
    @SerializedName("dt") val dt: Long,
    @SerializedName("sys") val sys: CurrentSys,
    @SerializedName("timezone") val timezone: Long,
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("cod") val cod: Long
)

data class CurrentCoord(
    @SerializedName("lon") val lon: Double,
    @SerializedName("lat") val lat: Double
)

data class CurrentWeather(
    @SerializedName("id") val id: Long,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class CurrentMain(
    @SerializedName("temp") val temp: Float,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Float, // Changed from Long to Float
    @SerializedName("temp_max") val tempMax: Float, // Changed from Long to Float
    @SerializedName("pressure") val pressure: Long,
    @SerializedName("humidity") val humidity: Long,
    @SerializedName("sea_level") val seaLevel: Long,
    @SerializedName("grnd_level") val grndLevel: Long
)


data class CurrentWind(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val deg: Long,
    @SerializedName("gust") val gust: Double
)

data class CurrentClouds(
    @SerializedName("all") val all: Long
)

data class CurrentSys(
    @SerializedName("type") val type: Long,
    @SerializedName("id") val id: Long,
    @SerializedName("country") val country: String,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)