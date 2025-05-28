package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val requestBatteryOptimization =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "Battery optimization exemption granted")
            } else {
                Log.d(TAG, "Battery optimization exemption not granted")
            }
        }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "AppPreferences"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_USER_SET_LANGUAGE = "user_set_language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language: check SharedPreferences, fallback to device language and save it
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val userSetLanguage = sharedPreferences.getBoolean(PREF_USER_SET_LANGUAGE, false)
        var savedLanguage = sharedPreferences.getString(PREF_LANGUAGE, null)
        if (savedLanguage == null || !userSetLanguage) {
            savedLanguage = getDeviceLanguage()
            sharedPreferences.edit {
                putString(PREF_LANGUAGE, savedLanguage)
                putBoolean(PREF_USER_SET_LANGUAGE, false)
            }
            Log.d(TAG, "No user-set language, using device language: $savedLanguage")
        }
        updateLocale(savedLanguage)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout = binding.drawerLayout
        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Get latitude, longitude, and destination from Intent
        val latitude = intent.getDoubleExtra("latitude", Double.NaN)
        val longitude = intent.getDoubleExtra("longitude", Double.NaN)
        val destinationId = intent.getIntExtra("destinationId", R.id.nav_home)

        // Validate coordinates
        val bundle = if (latitude.isNaN() || longitude.isNaN() || latitude == 0.0 || longitude == 0.0 ||
            latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            Log.e(TAG, "Invalid coordinates received: latitude=$latitude, longitude=$longitude")
            Log.w(TAG, "Using default coordinates: Cairo (30.0444, 31.2357)")
            Bundle().apply {
                putDouble("latitude", 30.0444)
                putDouble("longitude", 31.2357)
            }
        } else {
            Log.d(TAG, "Received valid coordinates: latitude=$latitude, longitude=$longitude")
            Bundle().apply {
                putDouble("latitude", latitude)
                putDouble("longitude", longitude)
            }
        }

        // Set navigation graph with arguments
        navController.setGraph(R.navigation.mobile_navigation, bundle)

        // Only navigate if we're not already on the destination
        if (navController.currentDestination?.id != destinationId) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_home, false)
                .setLaunchSingleTop(true)
                .build()
            navController.navigate(destinationId, null, navOptions)
        }

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_settings, R.id.nav_fav, R.id.nav_alert), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Debug navigation events
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navigated to: ${destination.id}")
        }

        // Request battery optimization exemption
        requestBatteryOptimizationExemption()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val userSetLanguage = sharedPreferences.getBoolean(PREF_USER_SET_LANGUAGE, false)
        val deviceLanguage = getDeviceLanguage()
        val savedLanguage = sharedPreferences.getString(PREF_LANGUAGE, deviceLanguage) ?: deviceLanguage

        if (!userSetLanguage && savedLanguage != deviceLanguage) {
            // Update language to device language and save to SharedPreferences
            sharedPreferences.edit {
                putString(PREF_LANGUAGE, deviceLanguage)
                putBoolean(PREF_USER_SET_LANGUAGE, false)
            }
            updateLocale(deviceLanguage)
            Log.d(TAG, "Device language changed, updated to: $deviceLanguage")
            restartActivity(findNavController(R.id.nav_host_fragment_content_main).currentDestination?.id ?: R.id.nav_home)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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

    private fun getDeviceLanguage(): String {
        val deviceLocale = Locale.getDefault().language
        return when (deviceLocale) {
            "ar" -> "arabic"
            else -> "english"
        }
    }

    fun updateLocale(languageCode: String) {
        val locale = when (languageCode) {
            "arabic" -> Locale("ar")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun restartActivity(currentDestinationId: Int = R.id.nav_settings) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("destinationId", currentDestinationId)
        finish()
        startActivity(intent)
    }
}