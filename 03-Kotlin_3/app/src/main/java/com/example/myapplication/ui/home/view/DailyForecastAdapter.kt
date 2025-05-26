package com.example.myapplication.ui.home.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemDailyForecastBinding
import com.example.weatherapp.model.pojos.WeatherData
import java.text.SimpleDateFormat
import java.util.*

class DailyForecastAdapter(
    private var dailyForecast: List<WeatherData>,
    private var tempUnit: String
) : RecyclerView.Adapter<DailyForecastAdapter.DailyForecastViewHolder>() {

    inner class DailyForecastViewHolder(val binding: ItemDailyForecastBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        val forecast = dailyForecast[position]
        holder.binding.apply {
            // Format date to show day of the week (e.g., "Monday")
            dayTextView.text = formatDate(forecast.dtTxt)

            // Set weather description
            weatherDescriptionTextView.text = forecast.weather.firstOrNull()?.description?.replaceFirstChar {
                it.titlecase()
            } ?: ""

            // Set max and min temperatures (same value) based on tempUnit
            val unitSymbol = when (tempUnit) {
                "celsius" -> "°C"
                "fahrenheit" -> "°F"
                "kelvin" -> "K"
                else -> "°C"
            }
            tempMaxTextView.text = "${forecast.main.tempMax.toInt()}$unitSymbol"
            tempMinTextView.text = "${forecast.main.tempMax.toInt()}$unitSymbol"

            // Load weather icon
            forecast.weather.firstOrNull()?.icon?.let { iconCode ->
                loadWeatherIcon(weatherIcon, iconCode)
            }
        }
    }

    override fun getItemCount() = dailyForecast.size

    fun updateData(newData: List<WeatherData>, newTempUnit: String) {
        dailyForecast = newData
        tempUnit = newTempUnit
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun loadWeatherIcon(imageView: android.widget.ImageView, iconCode: String) {
        val iconUrl = "ic_$iconCode"
        val icon = imageView.context.resources.getIdentifier(iconUrl, "drawable", imageView.context.packageName)
        Glide.with(imageView.context)
            .load(icon)
            .placeholder(R.drawable.ic_weather_placeholder)
            .error(R.drawable.ic_weather_error)
            .into(imageView)
    }
}