package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityInitBinding
import com.example.myapplication.utils.LocationProvider
import com.example.myapplication.utils.OsmMapActivity
import java.util.*

class InitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInitBinding
    private lateinit var locationProvider: LocationProvider
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "InitActivity"
        private const val PREFS_NAME = "AppPreferences"
        private const val PREF_LATITUDE = "pref_latitude"
        private const val PREF_LONGITUDE = "pref_longitude"
        private const val PREF_LOCATION_TYPE = "location_type"
        private const val PREF_TEMP_UNIT = "temp_unit"
        private const val PREF_WIND_UNIT = "wind_unit"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_NOTIFICATIONS = "notifications_enabled"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            Log.d(TAG, "Location permissions granted, proceeding with GPS selection")
            handleGpsSelection()
        } else {
            Log.w(TAG, "Location permissions denied")
            Toast.makeText(
                this,
                "Location permission denied. Please grant permission or select a location from the map.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val mapActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Map activity result received with code: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val lat = data.getDoubleExtra("lat", Double.NaN)
                val lon = data.getDoubleExtra("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN() || lat == 0.0 || lon == 0.0 ||
                    lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                    Log.e(TAG, "Invalid map coordinates received: lat=$lat, lon=$lon")
                    Toast.makeText(this, "Invalid location selected. Please try again.", Toast.LENGTH_LONG).show()
                    return@let
                }
                sharedPreferences.edit()
                    .putFloat(PREF_LATITUDE, lat.toFloat())
                    .putFloat(PREF_LONGITUDE, lon.toFloat())
                    .apply()
                Log.d(TAG, "Map coordinates match SharedPreferences: lat=$lat, lon=$lon")
                sharedPreferences.edit()
                    .putString(PREF_LOCATION_TYPE, "map")
                    .putString(PREF_TEMP_UNIT, sharedPreferences.getString(PREF_TEMP_UNIT, "celsius") ?: "celsius")
                    .putString(PREF_WIND_UNIT, sharedPreferences.getString(PREF_WIND_UNIT, "m/s") ?: "m/s")
                    .putString(PREF_LANGUAGE, getDeviceLanguage())
                    .putBoolean(PREF_NOTIFICATIONS, sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true))
                    .apply()
                Log.d(TAG, "Map location selected: Latitude=$lat, Longitude=$lon")
                val location = Location("map").apply {
                    latitude = lat
                    longitude = lon
                }
                navigateToMainActivity(location)
            } ?: run {
                Log.e(TAG, "No data returned from OsmMapActivity")
                Toast.makeText(this, "No location data received. Please try again.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(TAG, "Map activity cancelled or failed with result code: ${result.resultCode}")
            Toast.makeText(this, "Location selection cancelled. Please select a location.", Toast.LENGTH_SHORT).show()
        }
    }

    private val enableLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d(TAG, "Returned from location settings")
        if (locationProvider.isLocationEnabled()) {
            handleGpsSelection()
        } else {
            Log.w(TAG, "Location services still disabled after settings")
            showLocationDisabledDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        locationProvider = LocationProvider(this)

        binding.buttonGps.setOnClickListener {
            Log.d(TAG, "GPS button clicked")
            handleGpsSelection()
        }

        binding.buttonMap.setOnClickListener {
            Log.d(TAG, "Map button clicked")
            val intent = Intent(this, OsmMapActivity::class.java).apply {
                putExtra("source", "init")
            }
            mapActivityLauncher.launch(intent)
        }
    }

    private fun handleGpsSelection() {
        Log.d(TAG, "Handling GPS selection")
        if (!locationProvider.hasLocationPermission()) {
            Log.d(TAG, "Requesting location permissions")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (!locationProvider.isLocationEnabled()) {
            Log.w(TAG, "Location services disabled")
            showLocationDisabledDialog()
            return
        }

        locationProvider.getLocation(
            onSuccess = { location ->
                Log.d(TAG, "GPS location retrieved: Latitude=${location.latitude}, Longitude=${location.longitude}")
                sharedPreferences.edit()
                    .putFloat(PREF_LATITUDE, location.latitude.toFloat())
                    .putFloat(PREF_LONGITUDE, location.longitude.toFloat())
                    .apply()
                saveLocationTypeAndNavigate("gps", location)
            },
            onFailure = {
                Log.w(TAG, "Failed to retrieve GPS location")
                Toast.makeText(this, "Unable to retrieve location. Please try again or use map selection.", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showLocationDisabledDialog() {
        Log.d(TAG, "Showing location disabled dialog")
        AlertDialog.Builder(this)
            .setTitle("Location Services Disabled")
            .setMessage("Please enable location services to use GPS mode.")
            .setPositiveButton("Enable") { _, _ ->
                Log.d(TAG, "User chose to enable location services")
                enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Close") { dialog, _ ->
                Log.d(TAG, "User closed location disabled dialog")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun getDeviceLanguage(): String {
        val deviceLocale = Locale.getDefault().language
        return when (deviceLocale) {
            "ar" -> "arabic"
            else -> "english"
        }
    }

    private fun saveLocationTypeAndNavigate(locationType: String, location: Location? = null) {
        Log.d(TAG, "Saving location type: $locationType")
        sharedPreferences.edit()
            .putString(PREF_LOCATION_TYPE, locationType)
            .putString(PREF_TEMP_UNIT, sharedPreferences.getString(PREF_TEMP_UNIT, "celsius") ?: "celsius")
            .putString(PREF_WIND_UNIT, sharedPreferences.getString(PREF_WIND_UNIT, "m/s") ?: "m/s")
            .putString(PREF_LANGUAGE, getDeviceLanguage())
            .putBoolean(PREF_NOTIFICATIONS, sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true))
            .apply()
        navigateToMainActivity(location)
    }

    private fun navigateToMainActivity(location: Location? = null) {
        Log.d(TAG, "Navigating to MainActivity with location: ${location?.latitude}, ${location?.longitude}")
        val intent = Intent(this, MainActivity::class.java).apply {
            if (location != null) {
                putExtra("latitude", location.latitude)
                putExtra("longitude", location.longitude)
                Log.d(TAG, "Sending coordinates to MainActivity: latitude=${location.latitude}, longitude=${location.longitude}")
            } else {
                Log.w(TAG, "No location provided, sending no coordinates")
            }
        }
        startActivity(intent)
        finish()
    }
}