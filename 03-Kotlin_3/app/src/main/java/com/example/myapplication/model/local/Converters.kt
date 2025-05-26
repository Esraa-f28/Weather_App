package com.example.myapplication.model.local

import androidx.room.TypeConverter
import com.example.weatherapp.model.pojos.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromWeatherDataList(value: List<WeatherData>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeatherDataList(value: String): List<WeatherData> {
        val listType = object : TypeToken<List<WeatherData>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromCity(value: City?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCity(value: String): City? {
        return gson.fromJson(value, City::class.java)
    }

    @TypeConverter
    fun fromMain(value: Main?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMain(value: String): Main? {
        return gson.fromJson(value, Main::class.java)
    }

    @TypeConverter
    fun fromWeatherList(value: List<Weather>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeatherList(value: String): List<Weather> {
        val listType = object : TypeToken<List<Weather>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromWind(value: Wind?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWind(value: String): Wind? {
        return gson.fromJson(value, Wind::class.java)
    }

    @TypeConverter
    fun fromSys(value: Sys?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSys(value: String): Sys? {
        return gson.fromJson(value, Sys::class.java)
    }

    @TypeConverter
    fun fromCoord(value: Coord?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCoord(value: String): Coord? {
        return gson.fromJson(value, Coord::class.java)
    }

    @TypeConverter
    fun fromCurrentWeatherResponse(value: CurrentWeatherResponse?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentWeatherResponse(value: String): CurrentWeatherResponse? {
        return gson.fromJson(value, CurrentWeatherResponse::class.java)
    }

    @TypeConverter
    fun fromCurrentCoord(value: CurrentCoord?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentCoord(value: String): CurrentCoord? {
        return gson.fromJson(value, CurrentCoord::class.java)
    }

    @TypeConverter
    fun fromCurrentWeatherList(value: List<CurrentWeather>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentWeatherList(value: String): List<CurrentWeather> {
        val listType = object : TypeToken<List<CurrentWeather>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromCurrentMain(value: CurrentMain?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentMain(value: String): CurrentMain? {
        return gson.fromJson(value, CurrentMain::class.java)
    }

    @TypeConverter
    fun fromCurrentWind(value: CurrentWind?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentWind(value: String): CurrentWind? {
        return gson.fromJson(value, CurrentWind::class.java)
    }

    @TypeConverter
    fun fromCurrentClouds(value: CurrentClouds?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentClouds(value: String): CurrentClouds? {
        return gson.fromJson(value, CurrentClouds::class.java)
    }

    @TypeConverter
    fun fromCurrentSys(value: CurrentSys?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCurrentSys(value: String): CurrentSys? {
        return gson.fromJson(value, CurrentSys::class.java)
    }
}