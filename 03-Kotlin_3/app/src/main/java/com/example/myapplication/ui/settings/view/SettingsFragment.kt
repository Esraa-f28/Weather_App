package com.example.myapplication.ui.settings.view

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.utils.OsmMapActivity

class SettingsFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val PREF_LOCATION_TYPE = "location_type"
        private const val PREF_TEMP_UNIT = "temp_unit"
        private const val PREF_WIND_UNIT = "wind_unit"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_NOTIFICATIONS = "notifications_enabled"
        private const val PREF_LATITUDE = "pref_latitude"
        private const val PREF_LONGITUDE = "pref_longitude"
        private const val REQUEST_CODE_MAP = 1001
        private const val TAG = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private var isUpdatingWindUnit = false
    private var isUpdatingTempUnit = false
    private var selectedLocationType: String = "gps"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadSettings()
        setupListeners()
        return binding.root
    }

    private fun loadSettings() {
        val locationType = sharedPreferences.getString(PREF_LOCATION_TYPE, "gps") ?: "gps"
        selectedLocationType = locationType
        when (locationType) {
            "gps" -> binding.rbGpsLocation.isChecked = true
            "map" -> binding.rbMapLocation.isChecked = true
        }

        val tempUnit = sharedPreferences.getString(PREF_TEMP_UNIT, "celsius") ?: "celsius"
        when (tempUnit) {
            "kelvin" -> binding.rbKelvin.isChecked = true
            "celsius" -> binding.rbCelsius.isChecked = true
            "fahrenheit" -> binding.rbFahrenheit.isChecked = true
        }

        val windUnit = sharedPreferences.getString(PREF_WIND_UNIT, "m/s") ?: "m/s"
        when (windUnit) {
            "m/s" -> binding.rbMeterSec.isChecked = true
            "mph" -> binding.rbMilesHour.isChecked = true
        }

        val language = sharedPreferences.getString(PREF_LANGUAGE, "english") ?: "english"
        when (language) {
            "arabic" -> binding.rbArabic.isChecked = true
            "english" -> binding.rbEnglish.isChecked = true
        }

        val notificationsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true)
        if (notificationsEnabled) {
            binding.rbEnableNotifications.isChecked = true
        } else {
            binding.rbDisableNotifications.isChecked = true
        }
    }

    private fun setupListeners() {
        // Temperature unit listener
        binding.tempUnitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isUpdatingTempUnit) return@setOnCheckedChangeListener

            val tempUnit = when (checkedId) {
                binding.rbCelsius.id -> "celsius"
                binding.rbFahrenheit.id -> "fahrenheit"
                binding.rbKelvin.id -> "kelvin"
                else -> return@setOnCheckedChangeListener
            }
            saveTempAndWindUnits(tempUnit)
        }

        // Wind unit listener
        binding.windUnitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isUpdatingWindUnit) return@setOnCheckedChangeListener

            val windUnit = when (checkedId) {
                binding.rbMeterSec.id -> "m/s"
                binding.rbMilesHour.id -> "mph"
                else -> return@setOnCheckedChangeListener
            }
            saveTempAndWindUnits(windUnit = windUnit)
        }

        // Location listener
        binding.locationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedLocationType = when (checkedId) {
                binding.rbGpsLocation.id -> "gps"
                binding.rbMapLocation.id -> "map"
                else -> "gps"
            }
            if (selectedLocationType == "map") {
                val intent = Intent(requireContext(), OsmMapActivity::class.java)
                intent.putExtra("source", "settings")
                startActivityForResult(intent, REQUEST_CODE_MAP)
            } else {
                sharedPreferences.edit()
                    .putString(PREF_LOCATION_TYPE, "gps")
                    .remove(PREF_LATITUDE)
                    .remove(PREF_LONGITUDE)
                    .apply()
                Log.d(TAG, "Saved location type: gps, cleared map coordinates")
            }
        }

        // Language listener
        binding.languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                binding.rbArabic.id -> "arabic"
                binding.rbEnglish.id -> "english"
                else -> "english"
            }
            sharedPreferences.edit()
                .putString(PREF_LANGUAGE, language)
                .apply()
            Log.d(TAG, "Saved language: $language")
        }

        // Notifications listener
        binding.notificationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val notificationsEnabled = checkedId == binding.rbEnableNotifications.id
            sharedPreferences.edit()
                .putBoolean(PREF_NOTIFICATIONS, notificationsEnabled)
                .apply()
            Log.d(TAG, "Saved notifications enabled: $notificationsEnabled")
        }
    }

    private fun saveTempAndWindUnits(tempUnit: String? = null, windUnit: String? = null) {
        isUpdatingWindUnit = true
        isUpdatingTempUnit = true

        val finalTempUnit = tempUnit ?: when (windUnit) {
            "m/s" -> "celsius"
            "mph" -> "fahrenheit"
            else -> sharedPreferences.getString(PREF_TEMP_UNIT, "celsius") ?: "celsius"
        }

        val finalWindUnit = windUnit ?: when (finalTempUnit) {
            "celsius" -> "m/s"
            "fahrenheit" -> "mph"
            else -> sharedPreferences.getString(PREF_WIND_UNIT, "m/s") ?: "m/s"
        }

        // Update UI for wind unit
        when (finalWindUnit) {
            "m/s" -> binding.rbMeterSec.isChecked = true
            "mph" -> binding.rbMilesHour.isChecked = true
        }

        // Update UI for temp unit
        when (finalTempUnit) {
            "kelvin" -> binding.rbKelvin.isChecked = true
            "celsius" -> binding.rbCelsius.isChecked = true
            "fahrenheit" -> binding.rbFahrenheit.isChecked = true
        }

        sharedPreferences.edit()
            .putString(PREF_TEMP_UNIT, finalTempUnit)
            .putString(PREF_WIND_UNIT, finalWindUnit)
            .apply()

        Log.d(TAG, "Saved temp unit: $finalTempUnit, wind unit: $finalWindUnit")

        isUpdatingWindUnit = false
        isUpdatingTempUnit = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAP && resultCode == Activity.RESULT_OK) {
            val lat = data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = data?.getDoubleExtra("lon", 0.0) ?: 0.0
            if (lat != 0.0 && lon != 0.0) {
                sharedPreferences.edit()
                    .putFloat(PREF_LATITUDE, lat.toFloat())
                    .putFloat(PREF_LONGITUDE, lon.toFloat())
                    .putString(PREF_LOCATION_TYPE, "map")
                    .apply()
                Log.d(TAG, "Saved map coordinates: lat=$lat, lon=$lon, location type: map")
            } else {
                Log.w(TAG, "Invalid coordinates received from OsmMapActivity")
                binding.rbGpsLocation.isChecked = true
                selectedLocationType = "gps"
                sharedPreferences.edit()
                    .putString(PREF_LOCATION_TYPE, "gps")
                    .remove(PREF_LATITUDE)
                    .remove(PREF_LONGITUDE)
                    .apply()
                Log.d(TAG, "Reverted to GPS, cleared map coordinates")
            }
        } else {
            Log.w(TAG, "OsmMapActivity cancelled or failed")
            binding.rbGpsLocation.isChecked = true
            selectedLocationType = "gps"
            sharedPreferences.edit()
                .putString(PREF_LOCATION_TYPE, "gps")
                .remove(PREF_LATITUDE)
                .remove(PREF_LONGITUDE)
                .apply()
            Log.d(TAG, "Reverted to GPS, cleared map coordinates")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}