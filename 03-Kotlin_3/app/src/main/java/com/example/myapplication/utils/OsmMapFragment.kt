package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.example.myapplication.databinding.FragmentOsmMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class OsmMapFragment : Fragment() {

    private var _binding: FragmentOsmMapBinding? = null
    private val binding get() = _binding!!

    private var selectedMarker: Marker? = null
    private var selectedLocation: GeoPoint? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOsmMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize osmdroid config
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("prefs", 0))
        Configuration.getInstance().setUserAgentValue(requireContext().packageName)  // <-- set user agent here

        binding.mapview.setMultiTouchControls(true)
        binding.mapview.controller.setZoom(15.0)
        binding.mapview.controller.setCenter(GeoPoint(30.0444, 31.2357)) // Default Cairo
        // Set tap listener to select location
        binding.mapview.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val tappedGeoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                selectedLocation = tappedGeoPoint
                addOrUpdateMarker(tappedGeoPoint)

                Toast.makeText(requireContext(), "Location selected: ${tappedGeoPoint.latitude}, ${tappedGeoPoint.longitude}", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        binding.buttonConfirm.setOnClickListener {
            if (selectedLocation != null) {
                val resultBundle = Bundle().apply {
                    putDouble("lat", selectedLocation!!.latitude)
                    putDouble("lon", selectedLocation!!.longitude)
                }
                setFragmentResult("locationSelected", resultBundle)
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Please select a location on the map first", Toast.LENGTH_SHORT).show()
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
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//package com.example.myapplication.utils
//
//import android.app.AlertDialog
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewGroup
//import android.widget.EditText
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import com.example.myapplication.R
//import com.example.myapplication.databinding.FragmentOsmMapBinding
//import org.osmdroid.config.Configuration
//import org.osmdroid.util.GeoPoint
//import org.osmdroid.views.MapView
//import org.osmdroid.views.overlay.Marker
//import org.osmdroid.views.overlay.Overlay
//
//class OsmMapFragment : Fragment() {
//
//    private var _binding: FragmentOsmMapBinding? = null
//    private val binding get() = _binding!!
//
//    private var selectedMarker: Marker? = null
//    private var selectedLocation: GeoPoint? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentOsmMapBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Initialize osmdroid config
//        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("prefs", 0))
//        Configuration.getInstance().setUserAgentValue(requireContext().packageName)
//
//        binding.mapview.setMultiTouchControls(true)
//        binding.mapview.controller.setZoom(15.0)
//        binding.mapview.controller.setCenter(GeoPoint(30.0444, 31.2357)) // Default Cairo
//
//        // Set tap listener to select location
//        binding.mapview.overlays.add(object : Overlay() {
//            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
//                val tappedGeoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
//                selectedLocation = tappedGeoPoint
//                addOrUpdateMarker(tappedGeoPoint)
//
//                // Show location details dialog
//                showLocationDetailsDialog(tappedGeoPoint)
//                return true
//            }
//        })
//
//        binding.buttonConfirm.visibility = View.GONE // Hide confirm button as we'll use dialog
//    }
//
//    private fun showLocationDetailsDialog(geoPoint: GeoPoint) {
//        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_location_details, null)
//        val nameEditText = dialogView.findViewById<EditText>(R.id.editTextPlaceName)
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("Location Details")
//            .setView(dialogView)
//            .setPositiveButton("Add Favorite") { _, _ ->
//                val placeName = nameEditText.text.toString().ifEmpty {
//                    "Favorite at (${geoPoint.latitude}, ${geoPoint.longitude})"
//                }
//                val intent = Intent().apply {
//                    putExtra("latitude", geoPoint.latitude)
//                    putExtra("longitude", geoPoint.longitude)
//                    putExtra("placeName", placeName)
//                }
//                requireActivity().setResult(android.app.Activity.RESULT_OK, intent)
//                requireActivity().finish()
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun addOrUpdateMarker(geoPoint: GeoPoint) {
//        if (selectedMarker == null) {
//            selectedMarker = Marker(binding.mapview).apply {
//                position = geoPoint
//                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//            }
//            binding.mapview.overlays.add(selectedMarker)
//        } else {
//            selectedMarker!!.position = geoPoint
//        }
//        binding.mapview.invalidate()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        binding.mapview.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        binding.mapview.onPause()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}