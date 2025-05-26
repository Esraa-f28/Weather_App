package com.example.myapplication.ui.home.view

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.model.local.LocalDataSource
import com.example.myapplication.model.remote.RemoteDataSource
import com.example.myapplication.model.repo.Repository
import com.example.myapplication.ui.home.viewmodel.HomeViewModel
import com.example.myapplication.ui.home.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Initialize HomeViewModel
        val factory = ViewModelFactory(
            Repository.getInstance(
                remoteDataSource = RemoteDataSource(),
                localDataSource = LocalDataSource.getInstance(requireContext())
            )
        )
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Log saved settings from SharedPreferences
        logSavedSettings()

        // Setup RecyclerViews with adapter initialization
        setupRecyclerViews()

        // Observe ViewModel data
        observeViewModel()

        // Register SharedPreferences listener
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "location_type" || key == "pref_latitude" || key == "pref_longitude") {
                Log.d("HomeFragment", "SharedPreferences changed: $key")
                fetchWeatherDataBasedOnSettings()
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)

        // Observe settings for runtime changes
        viewModel.tempUnit.observe(viewLifecycleOwner) { tempUnit ->
            Log.d("HomeFragment", "Temperature unit changed: $tempUnit")
            fetchWeatherDataBasedOnSettings()
            // Update adapters with new temp unit
            viewModel.hourlyForecast.value?.let { data ->
                hourlyAdapter.updateData(data, tempUnit ?: getTempUnit())
                Log.d("HomeFragment", "Updated hourly adapter with $tempUnit")
            } ?: run {
                hourlyAdapter.updateData(emptyList(), tempUnit ?: getTempUnit())
                Log.w("HomeFragment", "No hourly forecast data, initialized empty adapter with $tempUnit")
            }
            viewModel.dailyForecast.value?.let { data ->
                dailyAdapter.updateData(data, tempUnit ?: getTempUnit())
                Log.d("HomeFragment", "Updated daily adapter with $tempUnit")
            } ?: run {
                dailyAdapter.updateData(emptyList(), tempUnit ?: getTempUnit())
                Log.w("HomeFragment", "No daily forecast data, initialized empty adapter with $tempUnit")
            }
        }
        viewModel.language.observe(viewLifecycleOwner) { language ->
            Log.d("HomeFragment", "Language changed: $language")
            fetchWeatherDataBasedOnSettings()
        }
        viewModel.windUnit.observe(viewLifecycleOwner) { windUnit ->
            Log.d("HomeFragment", "Wind unit changed: $windUnit")
            // Trigger UI update for current weather
            viewModel.currentWeather.value?.let { updateCurrentWeather(it) }
        }

        // Trigger initial fetch using saved settings
        fetchWeatherDataBasedOnSettings()

        return binding.root
    }

    private fun logSavedSettings() {
        Log.d("HomeFragment", "Saved settings from SharedPreferences:")
        Log.d("HomeFragment", "Location Type: ${getLocationType()}")
        Log.d("HomeFragment", "Latitude: ${getLatitude()}")
        Log.d("HomeFragment", "Longitude: ${getLongitude()}")
        Log.d("HomeFragment", "Temperature Unit: ${getTempUnit()}")
        Log.d("HomeFragment", "Wind Unit: ${getWindUnit()}")
        Log.d("HomeFragment", "Language: ${getLanguage()}")
        Log.d("HomeFragment", "Notifications Enabled: ${getNotificationsEnabled()}")
    }

    private fun getLocationType(): String {
        return sharedPreferences.getString("location_type", "gps") ?: "gps"
    }

    private fun getTempUnit(): String {
        return sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
    }

    private fun getWindUnit(): String {
        return sharedPreferences.getString("wind_unit", "m/s") ?: "m/s"
    }

    private fun getLanguage(): String {
        return sharedPreferences.getString("language", "english") ?: "english"
    }

    private fun getNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    private fun getLatitude(): Float {
        return sharedPreferences.getFloat("pref_latitude", 0f)
    }

    private fun getLongitude(): Float {
        return sharedPreferences.getFloat("pref_longitude", 0f)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun fetchWeatherDataBasedOnSettings() {
        val (latitude, longitude) = getCoordinates()
        Log.d("HomeFragment", "Using coordinates: latitude=$latitude, longitude=$longitude")

        val tempUnit = getTempUnit()
        val units = when (tempUnit) {
            "celsius" -> "metric"
            "fahrenheit" -> "imperial"
            "kelvin" -> "standard"
            else -> "metric" // Default to Celsius
        }
        val language = getLanguage()
        val langCode = if (language == "arabic") "ar" else "en"

        // Fetch data through ViewModel, passing network availability
        Log.d("HomeFragment", "Fetching weather with units: $units (tempUnit: $tempUnit), lang: $langCode, lat: $latitude, lon: $longitude, network: ${isNetworkAvailable()}")
        viewModel.fetchWeatherData(
            latitude = latitude,
            longitude = longitude,
            apiKey = "36f28ef1ca3386a0cd3bff9801d97e53",
            units = units,
            lang = langCode,
            isNetworkAvailable = isNetworkAvailable()
        )
    }

    private fun getCoordinates(): Pair<Double, Double> {
        val locationType = getLocationType()

        // Try to get coordinates from arguments first
        val argsLat = arguments?.getFloat("latitude")?.toDouble()
        val argsLon = arguments?.getFloat("longitude")?.toDouble()

        if (argsLat != null && argsLon != null) {
            Log.d("HomeFragment", "Using coordinates from arguments: lat=$argsLat, lon=$argsLon")
            return Pair(argsLat, argsLon)
        }

        // Handle based on location type
        return when (locationType) {
            "gps" -> {
                Log.d("HomeFragment", "Location type is GPS")
                val prefLat = getLatitude().toDouble()
                val prefLon = getLongitude().toDouble()
                if (prefLat != 0.0 && prefLon != 0.0) {
                    Log.d("HomeFragment", "Using saved GPS coordinates: $prefLat, $prefLon")
                    Pair(prefLat, prefLon)
                } else {
                    Log.w("HomeFragment", "No saved GPS coordinates, using default (Cairo)")
                    Pair(30.0131, 31.2089) // Default to Cairo
                }
            }
            "map" -> {
                val prefLat = getLatitude().toDouble()
                val prefLon = getLongitude().toDouble()
                if (prefLat != 0.0 && prefLon != 0.0) {
                    Log.d("HomeFragment", "Using coordinates from SharedPreferences for map: $prefLat, $prefLon")
                    Pair(prefLat, prefLon)
                } else {
                    Log.w("HomeFragment", "No saved map coordinates, using default (Cairo)")
                    Pair(30.0131, 31.2089) // Default to Cairo
                }
            }
            else -> {
                Log.w("HomeFragment", "Unknown location type, using default coordinates")
                Pair(30.0131, 31.2089) // Default to Cairo
            }
        }
    }

    private fun setupRecyclerViews() {
        val tempUnit = getTempUnit()
        Log.d("HomeFragment", "Initializing adapters with tempUnit: $tempUnit")
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

    private fun observeViewModel() {
        viewModel.currentWeather.observe(viewLifecycleOwner) { currentWeather ->
            currentWeather?.let { updateCurrentWeather(it) }
        }

        viewModel.hourlyForecast.observe(viewLifecycleOwner) { hourlyForecast ->
            val tempUnit = getTempUnit()
            hourlyAdapter.updateData(hourlyForecast, tempUnit)
            binding.rvHourlyForecast.adapter = hourlyAdapter
            Log.i("HomeFragment", "Hourly forecast updated: ${hourlyForecast.size} items with tempUnit: $tempUnit")
        }

        viewModel.dailyForecast.observe(viewLifecycleOwner) { dailyForecast ->
            val tempUnit = getTempUnit()
            dailyAdapter.updateData(dailyForecast, tempUnit)
            binding.rvDailyForecast.adapter = dailyAdapter
            Log.i("HomeFragment", "Daily forecast updated: ${dailyForecast.size} items with tempUnit: $tempUnit")
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                Log.e("HomeFragment", "Error: $error")
            }
        }
    }

    private fun updateCurrentWeather(currentWeather: com.example.weatherapp.model.pojos.CurrentWeatherResponse) {
        binding.tvCity.text = currentWeather.name
        val tempUnit = getTempUnit()
        val unitSymbol = when (tempUnit) {
            "celsius" -> "°C"
            "fahrenheit" -> "°F"
            "kelvin" -> "K"
            else -> "°C"
        }
        binding.tvTemperature.text = "${currentWeather.main.temp.toInt()}$unitSymbol"
        Log.d("HomeFragment", "Displaying temperature: ${currentWeather.main.temp.toInt()}$unitSymbol")

        binding.tvWeatherDisc.text = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { char ->
            char.titlecase()
        } ?: "N/A"
        binding.tvPressure.text = "${currentWeather.main.pressure} hPa"
        binding.tvHumidity.text = "${currentWeather.main.humidity} %"
        val windUnit = getWindUnit()
        val windSpeed = if (windUnit == "mph") {
            (currentWeather.wind.speed * 2.23694).toInt() // Convert m/s to mph
        } else {
            currentWeather.wind.speed.toInt()
        }
        binding.tvWind.text = "$windSpeed $windUnit"
        Log.d("HomeFragment", "Displaying wind: $windSpeed $windUnit")

        binding.tvClouds.text = "${currentWeather.clouds.all} %"

        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timestamp = currentWeather.dt * 1000L
        val date = Date(timestamp)
        binding.tvData.text = dateFormat.format(date)
        binding.tvTime.text = timeFormat.format(date)

        currentWeather.weather.firstOrNull()?.icon?.let { iconCode ->
            loadWeatherIcon(binding.ivWeatherIcon, iconCode)
            Log.d("HomeFragment", "Icon code: $iconCode")
        } ?: Log.w("HomeFragment", "No icon code available for current weather")

        Log.i("HomeFragment", "Displaying weather for city: ${currentWeather.name}, coordinates: latitude=${currentWeather.coord.lat}, longitude=${currentWeather.coord.lon}")
    }

    private fun loadWeatherIcon(imageView: android.widget.ImageView, iconCode: String) {
        val iconUrl = "ic_$iconCode"
        val icon=context?.resources?.getIdentifier(iconUrl,"drawable",context?.packageName)
        Glide.with(imageView.context)
            .load(icon)
            .placeholder(R.drawable.ic_weather_placeholder)
            .error(R.drawable.ic_weather_error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(DecodeFormat.PREFER_ARGB_8888)
            .into(imageView)
        Log.d("loadWeatherIcon", "Loading icon: $iconUrl")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister SharedPreferences listener
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
        _binding = null
    }
}