package com.example.myapplication

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val requestBatteryOptimization =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            // Optional: Handle the result if needed
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "Battery optimization exemption granted")
            } else {
                Log.d(TAG, "Battery optimization exemption not granted")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout = binding.drawerLayout
        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Get latitude and longitude from Intent
        val latitude = intent.getDoubleExtra("latitude", Double.NaN)
        val longitude = intent.getDoubleExtra("longitude", Double.NaN)

        // Validate coordinates
        if (latitude.isNaN() || longitude.isNaN() || latitude == 0.0 || longitude == 0.0 ||
            latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            Log.e("Main/Activity", "Invalid coordinates received: latitude=$latitude, longitude=$longitude")
            // Fallback to default coordinates (Cairo)
            val bundle = Bundle().apply {
                putDouble("latitude", 30.0444)
                putDouble("longitude", 31.2357)
            }
            Log.w("MainActivity", "Using default coordinates: Cairo (30.0444, 31.2357)")
            navController.setGraph(R.navigation.mobile_navigation, bundle)
        } else {
            Log.d("MainActivity", "Received valid coordinates: latitude=$latitude, longitude=$longitude")
            // Pass latitude and longitude to HomeFragment
            val bundle = Bundle().apply {
                putDouble("latitude", latitude)
                putDouble("longitude", longitude)
            }
            navController.setGraph(R.navigation.mobile_navigation, bundle)
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_fav, R.id.nav_alert
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Requesting battery optimization exemption")
                requestBatteryOptimization.launch(
                    Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                )
            } else {
                Log.d(TAG, "Battery optimization already disabled")
            }
        }
    }

}

//package com.example.myapplication
//
//import android.os.Bundle
//import android.util.Log
//import android.view.Menu
//import androidx.appcompat.app.AppCompatActivity
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.navigateUp
//import androidx.navigation.ui.setupActionBarWithNavController
//import androidx.navigation.ui.setupWithNavController
//import com.example.myapplication.databinding.ActivityMainBinding
//import com.example.myapplication.model.local.LocalDataSource
//import com.example.myapplication.model.remote.RemoteDataSource
//import com.example.myapplication.model.repo.Repository
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var appBarConfiguration: AppBarConfiguration
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var repository: Repository
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setSupportActionBar(binding.appBarMain.toolbar)
//
//        val drawerLayout = binding.drawerLayout
//        val navView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//
//        // Initialize Repository
//        val localDataSource = LocalDataSource.getInstance(this)
//        val remoteDataSource = RemoteDataSource()// Replace with actual implementation
//        repository = Repository.getInstance(remoteDataSource, localDataSource)
//
//        // Get latitude and longitude from Intent
//        val latitude = intent.getDoubleExtra("latitude", Double.NaN)
//        val longitude = intent.getDoubleExtra("longitude", Double.NaN)
//        val apiKey = "36f28ef1ca3386a0cd3bff9801d97e53" // Replace with your actual API key

        // Validate coordinates and perform fetch-store-retrieve test
//        CoroutineScope(Dispatchers.Main).launch {
//            val (finalLatitude, finalLongitude) = if (latitude.isNaN() || longitude.isNaN() ||
//                latitude == 0.0 || longitude == 0.0 ||
//                latitude < -90.0 || latitude > 90.0 ||
//                longitude < -180.0 || longitude > 180.0) {
//                Log.e("MainActivity", "Invalid coordinates received: latitude=$latitude, longitude=$longitude")
//                Log.w("MainActivity", "Using default coordinates: Cairo (30.0444, 31.2357)")
//                Pair(30.0444, 31.2357)
//            } else {
//                Log.d("MainActivity", "Received valid coordinates: latitude=$latitude, longitude=$longitude")
//                Pair(latitude, longitude)
//            }
//
//            // Pass coordinates to HomeFragment
//            val bundle = Bundle().apply {
//                putDouble("latitude", finalLatitude)
//                putDouble("longitude", finalLongitude)
//            }
//            navController.setGraph(R.navigation.mobile_navigation, bundle)
//
//            // Test fetch, store, and retrieve
//            try {
//                // Fetch and store hourly forecast
//                val hourlyResult = repository.getHourlyForecast(
//                    latitude = finalLatitude,
//                    longitude = finalLongitude,
//                    apiKey = apiKey,
//                    cityId = 12345 // Example city ID
//                )
//                if (hourlyResult.isSuccess) {
//                    val hourlyResponse = hourlyResult.getOrNull()
//                    hourlyResponse?.let {
//                        Log.d("MainActivity", "Fetched Hourly Forecast: City=${it.city.name}, Temp=${it.list.firstOrNull()?.main?.temp}°C")
//                        // Retrieve by ID (assuming ID=1 for first entry)
//                        val localHourly = repository.getHourlyForecastLocal(id = 1)
//                        if (localHourly != null) {
//                            Log.d("MainActivity", "Retrieved Hourly Forecast: City=${localHourly.city.name}, Temp=${localHourly.list.firstOrNull()?.main?.temp}°C, ID=${localHourly.id}")
//                        } else {
//                            Log.e("MainActivity", "Failed to retrieve hourly forecast from local storage for ID=1")
//                        }
//                    }
//                } else {
//                    Log.e("MainActivity", "Failed to fetch hourly forecast: ${hourlyResult.exceptionOrNull()?.message}")
//                }
//
//                // Fetch and store current weather
//                val currentResult = repository.getCurrentWeather(
//                    latitude = finalLatitude,
//                    longitude = finalLongitude,
//                    apiKey = apiKey
//                )
//                if (currentResult.isSuccess) {
//                    val currentResponse = currentResult.getOrNull()
//                    currentResponse?.let {
//                        Log.d("MainActivity", "Fetched Current Weather: City=${it.name}, Temp=${it.main.temp}°C")
//                        // Retrieve by ID (assuming ID=1 for first entry)
//                        val localCurrent = repository.getCurrentWeatherLocal(id = 1)
//                        if (localCurrent != null) {
//                            Log.d("MainActivity", "Retrieved Current Weather: City=${localCurrent.name}, Temp=${localCurrent.main.temp}°C, ID=${localCurrent.id_current}")
//                        } else {
//                            Log.e("MainActivity", "Failed to retrieve current weather from local storage for ID=1")
//                        }
//                    }
//                } else {
//                    Log.e("MainActivity", "Failed to fetch current weather: ${currentResult.exceptionOrNull()?.message}")
//                }
//            } catch (e: Exception) {
//                Log.e("MainActivity", "Error during fetch/store/retrieve test: ${e.message}")
//            }
//        }
//
//        // Set up navigation
//        appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.nav_home, R.id.nav_settings, R.id.nav_slideshow, R.id.nav_alert
//            ), drawerLayout
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }
//}