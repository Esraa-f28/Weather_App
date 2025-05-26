package com.example.myapplication.ui.favourites.view

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.databinding.FavDetailsFragmentBinding
import com.example.myapplication.model.local.FavoritePlace
import com.example.myapplication.ui.favourites.viewmodel.FavViewModel
import com.example.myapplication.ui.home.view.DailyForecastAdapter
import com.example.myapplication.ui.home.view.HourlyForecastAdapter
import com.example.weatherapp.model.pojos.CurrentWeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavDetailsFragment : Fragment() {

    private var _binding: FavDetailsFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavViewModel by activityViewModels()
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var tempUnit: String
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FavDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"

        // Setup RecyclerViews
        setupRecyclerViews()

        // Observe selected place and fetch weather data
        viewModel.selectedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null) {
                binding.tvCity.text = place.city ?: "Unknown City"
                viewModel.fetchWeatherData(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    apiKey = "36f28ef1ca3386a0cd3bff9801d97e53", // Replace with your actual OpenWeatherMap API key
                    units = sharedPreferences.getString("temp_unit", "metric") ?: "metric",
                    lang = "en"
                )
            } else {
                Toast.makeText(context, "No place selected", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // Observe current weather
        viewModel.currentWeather.observe(viewLifecycleOwner) { currentWeather ->
            currentWeather?.let { updateCurrentWeather(it) }
        }

        // Observe hourly forecast
        viewModel.hourlyForecast.observe(viewLifecycleOwner) { hourlyForecast ->
            hourlyAdapter.updateData(hourlyForecast, tempUnit)
            binding.rvHourlyForecast.adapter = hourlyAdapter
            Log.i("FavDetailsFragment", "Hourly forecast updated: ${hourlyForecast.size} items with tempUnit: $tempUnit")
        }

        // Observe daily forecast
        viewModel.dailyForecast.observe(viewLifecycleOwner) { dailyForecast ->
            dailyAdapter.updateData(dailyForecast, tempUnit)
            binding.rvDailyForecast.adapter = dailyAdapter
            Log.i("FavDetailsFragment", "Daily forecast updated: ${dailyForecast.size} items with tempUnit: $tempUnit")
        }
    }

    private fun setupRecyclerViews() {
        hourlyAdapter = HourlyForecastAdapter(emptyList(), tempUnit)
        dailyAdapter = DailyForecastAdapter(emptyList(), tempUnit)

        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = hourlyAdapter
        }
        binding.rvDailyForecast.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = dailyAdapter
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun updateCurrentWeather(currentWeather: CurrentWeatherResponse) {
        val unitSymbol = when (tempUnit) {
            "celsius" -> "°C"
            "fahrenheit" -> "°F"
            "kelvin" -> "K"
            else -> "°C"
        }
        binding.tvTemperature.text = "${currentWeather.main.temp.toInt()}$unitSymbol"
        Log.d("FavDetailsFragment", "Displaying temperature: ${currentWeather.main.temp.toInt()}$unitSymbol")

        binding.tvWeatherDisc.text = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { char ->
            char.titlecase()
        } ?: "N/A"
        binding.tvPressure.text = "${currentWeather.main.pressure} hPa"
        binding.tvHumidity.text = "${currentWeather.main.humidity} %"

        val windUnit = sharedPreferences.getString("wind_unit", "m/s") ?: "m/s"
        val windSpeed = if (windUnit == "mph") {
            (currentWeather.wind.speed * 2.23694).toInt() // Convert m/s to mph
        } else {
            currentWeather.wind.speed.toInt()
        }
        binding.tvWind.text = "$windSpeed $windUnit"
        Log.d("FavDetailsFragment", "Displaying wind: $windSpeed $windUnit")

        binding.tvClouds.text = "${currentWeather.clouds.all} %"

        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timestamp = currentWeather.dt * 1000L
        val date = Date(timestamp)
        binding.tvData.text = dateFormat.format(date)
        binding.tvTime.text = timeFormat.format(date)

        currentWeather.weather.firstOrNull()?.icon?.let { iconCode ->
            loadWeatherIcon(binding.ivWeatherIcon, iconCode)
            Log.d("FavDetailsFragment", "Icon code: $iconCode")
        } ?: Log.w("FavDetailsFragment", "No icon code available for current weather")
    }

    private fun loadWeatherIcon(imageView: android.widget.ImageView, iconCode: String) {
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@4x.png"
        Glide.with(imageView.context)
            .load(iconUrl)
            .placeholder(R.drawable.ic_weather_placeholder)
            .error(R.drawable.ic_weather_error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(DecodeFormat.PREFER_ARGB_8888)
            .into(imageView)
        Log.d("FavDetailsFragment", "Loading icon: $iconUrl")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}