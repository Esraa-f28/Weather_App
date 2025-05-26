package com.example.myapplication.ui.home.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemHourlyForecastBinding
import com.example.weatherapp.model.pojos.WeatherData
import java.text.SimpleDateFormat
import java.util.*

class HourlyForecastAdapter(
    private var hourlyForecast: List<WeatherData>,
    private var tempUnit: String
) : RecyclerView.Adapter<HourlyForecastAdapter.HourlyForecastViewHolder>() {

    inner class HourlyForecastViewHolder(val binding: ItemHourlyForecastBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyForecastViewHolder {
        val binding = ItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyForecastViewHolder, position: Int) {
        val forecast = hourlyForecast[position]
        holder.binding.apply {
            // Format time (e.g., "14:00")
            tvTime.text = formatTime(forecast.dt)

            // Set temperature based on tempUnit
            val (temp, unitSymbol) = when (tempUnit) {
                "celsius" -> Pair(forecast.main.temp.toInt(), "°C")
                "fahrenheit" -> Pair(forecast.main.temp.toInt(), "°F")
                "kelvin" -> Pair(forecast.main.temp.toInt(), "K")
                else -> Pair(forecast.main.temp.toInt(), "°C")
            }
            tvTemperature.text = "$temp$unitSymbol"

            // Load weather icon
            forecast.weather.firstOrNull()?.icon?.let { iconCode ->
                loadWeatherIcon(ivWeatherIcon, iconCode)
            } ?: Log.w("HourlyForecastAdapter", "No icon code available")
        }
    }

    override fun getItemCount(): Int = hourlyForecast.size

    fun updateData(newData: List<WeatherData>, newTempUnit: String) {
        hourlyForecast = newData
        tempUnit = newTempUnit
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun loadWeatherIcon(imageView: ImageView, iconCode: String) {
        val iconUrl = "ic_$iconCode"
        val iconResId = imageView.context.resources.getIdentifier(iconUrl, "drawable", imageView.context.packageName)
        if (iconResId != 0) {
            Glide.with(imageView.context)
                .load(iconResId)
                .placeholder(R.drawable.ic_weather_placeholder)
                .error(R.drawable.ic_weather_error)
                .into(imageView)
        } else {
            Log.w("HourlyForecastAdapter", "Drawable not found for icon code: $iconCode")
        }
        Log.d("HourlyForecastAdapter", "Loading icon: $iconUrl")
    }
}