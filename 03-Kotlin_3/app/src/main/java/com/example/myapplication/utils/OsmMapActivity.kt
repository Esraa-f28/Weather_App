package com.example.myapplication.utils

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.FragmentOsmMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class OsmMapActivity : AppCompatActivity() {

    private lateinit var binding: FragmentOsmMapBinding
    private var selectedMarker: Marker? = null
    private var selectedLocation: GeoPoint? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val PREF_LATITUDE = "pref_latitude"
        private const val PREF_LONGITUDE = "pref_longitude"
        private const val PREF_LOCATION_TYPE = "location_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOsmMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Initialize osmdroid config
        Log.d("OsmMapActivity", "Initializing osmdroid configuration")
        Configuration.getInstance().load(this, getSharedPreferences("prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        // Setup MapView
        binding.mapview.setMultiTouchControls(true)
        binding.mapview.controller.setZoom(15.0)
        binding.mapview.controller.setCenter(GeoPoint(30.0444, 31.2357)) // Default Cairo
        Log.d("OsmMapActivity", "MapView initialized with center: Cairo (30.0444, 31.2357)")

        // Set tap listener to select location
        binding.mapview.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val tappedGeoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as? GeoPoint
                if (tappedGeoPoint == null) {
                    Log.e("OsmMapActivity", "Failed to get GeoPoint from tap at x=${e.x}, y=${e.y}")
                    Toast.makeText(this@OsmMapActivity, "Error selecting location. Please try again.", Toast.LENGTH_SHORT).show()
                    return false
                }
                selectedLocation = tappedGeoPoint
                addOrUpdateMarker(tappedGeoPoint)
                Toast.makeText(
                    this@OsmMapActivity,
                    "Location selected: ${tappedGeoPoint.latitude}, ${tappedGeoPoint.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("OsmMapActivity", "Tapped coordinates: lat=${tappedGeoPoint.latitude}, lon=${tappedGeoPoint.longitude}")
                return true
            }
        })

        // Confirm button to return selected location
        binding.buttonConfirm.setOnClickListener {
            if (selectedLocation != null) {
                // Validate coordinates
                if (selectedLocation!!.latitude == 0.0 && selectedLocation!!.longitude == 0.0) {
                    Log.e("OsmMapActivity", "Invalid coordinates selected: lat=0.0, lon=0.0")
                    Toast.makeText(this, "Invalid location selected. Please select a valid location.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (selectedLocation!!.latitude < -90.0 || selectedLocation!!.latitude > 90.0 ||
                    selectedLocation!!.longitude < -180.0 || selectedLocation!!.longitude > 180.0) {
                    Log.e("OsmMapActivity", "Coordinates out of valid range: lat=${selectedLocation!!.latitude}, lon=${selectedLocation!!.longitude}")
                    Toast.makeText(this, "Selected location is out of valid range. Please try again.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Save to SharedPreferences
                sharedPreferences.edit()
                    .putFloat(PREF_LATITUDE, selectedLocation!!.latitude.toFloat())
                    .putFloat(PREF_LONGITUDE, selectedLocation!!.longitude.toFloat())
                    .putString(PREF_LOCATION_TYPE, "map")
                    .apply()
                Log.d("OsmMapActivity", "Saved coordinates to SharedPreferences: lat=${selectedLocation!!.latitude}, lon=${selectedLocation!!.longitude}, location_type=map")

                val resultIntent = Intent().apply {
                    putExtra("lat", selectedLocation!!.latitude)
                    putExtra("lon", selectedLocation!!.longitude)
                    putExtra("placeName", "Selected Location (${selectedLocation!!.latitude}, ${selectedLocation!!.longitude})")
                }
                Log.d("OsmMapActivity", "Returning coordinates: lat=${selectedLocation!!.latitude}, lon=${selectedLocation!!.longitude}")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Log.w("OsmMapActivity", "No location selected")
                Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addOrUpdateMarker(geoPoint: GeoPoint) {
        if (selectedMarker == null) {
            selectedMarker = Marker(binding.mapview).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            binding.mapview.overlays.add(selectedMarker)
        } else {
            selectedMarker!!.position = geoPoint
        }
        binding.mapview.invalidate()
        Log.d("OsmMapActivity", "Marker updated at: lat=${geoPoint.latitude}, lon=${geoPoint.longitude}")
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
        Log.d("OsmMapActivity", "MapView resumed")
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
        Log.d("OsmMapActivity", "MapView paused")
    }
}