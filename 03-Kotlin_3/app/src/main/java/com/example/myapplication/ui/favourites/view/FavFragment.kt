package com.example.myapplication.ui.favourites.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentFavBinding
import com.example.myapplication.model.local.LocalDataSource
import com.example.myapplication.model.remote.RemoteDataSource
import com.example.myapplication.model.repo.Repository
import com.example.myapplication.ui.favourites.viewmodel.FavFactory
import com.example.myapplication.ui.favourites.viewmodel.FavViewModel
import com.example.myapplication.utils.OsmMapActivity
import java.util.Locale

class FavFragment : Fragment() {

    private var _binding: FragmentFavBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavViewModel by activityViewModels {
        val repository = Repository.getInstance(
            remoteDataSource = RemoteDataSource(),
            localDataSource = LocalDataSource.getInstance(requireContext())
        )
        FavFactory(repository)
    }
    private lateinit var adapter: FavAdapter

    private val mapActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val latitude = data.getDoubleExtra("lat", 0.0)
                val longitude = data.getDoubleExtra("lon", 0.0)
                val placeName = data.getStringExtra("placeName") ?: "Favorite at ($latitude, $longitude)"
                var city = placeName
                try {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        city = addresses[0].locality ?: placeName
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoding failed", e)
                }
                Log.d(TAG, "Received location: $placeName ($latitude, $longitude), City: $city")
                viewModel.addFavoritePlace(latitude, longitude, placeName, city)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavBinding.inflate(inflater, container, false)

        adapter = FavAdapter(
            onDeleteClick = { place ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Favorite")
                    .setMessage("Are you sure you want to delete ${place.city ?: place.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteFavoritePlace(place)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onItemClick = { place ->
                viewModel.setSelectedPlace(place)
                showFavDetails()
            }
        )
        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewFavorites.adapter = adapter

        viewModel.favoritePlaces.observe(viewLifecycleOwner) { places ->
            adapter.submitList(places)
        }

        binding.fabAddFavorite.setOnClickListener {
            Log.d(TAG, "FAB clicked")
            if (!isAdded || isDetached || activity == null) {
                Log.e(TAG, "Cannot open map: Fragment not in valid state")
                Toast.makeText(requireContext(), "Unable to open map", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Location permission granted, opening OsmMapActivity")
                val intent = Intent(requireContext(), OsmMapActivity::class.java)
                mapActivityLauncher.launch(intent)
            } else {
                Log.d(TAG, "Requesting location permission")
                Toast.makeText(
                    requireContext(),
                    "Location permission required to use the map",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        return binding.root
    }

    private fun showFavDetails() {
        if (isNetworkAvailable()) {
            Log.d(TAG, "Network available, navigating to FavDetailsFragment")
            binding.recyclerViewFavorites.visibility = View.GONE
            NavHostFragment.findNavController(this).navigate(R.id.favDetailsFragment)
        } else {
            Log.w(TAG, "No network connection")
            Toast.makeText(
                requireContext(),
                "No network connection. Please check your internet.",
                Toast.LENGTH_SHORT
            ).show()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdded) {
            parentFragmentManager.addOnBackStackChangedListener {
                if (isAdded && parentFragmentManager.backStackEntryCount == 0) {
                    binding.recyclerViewFavorites.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted, opening OsmMapActivity")
                if (!isAdded || isDetached || activity == null) {
                    Log.e(TAG, "Cannot open map after permission: Fragment not in valid state")
                    Toast.makeText(context, "Unable to open map", Toast.LENGTH_SHORT).show()
                    return
                }
                val intent = Intent(requireContext(), OsmMapActivity::class.java)
                mapActivityLauncher.launch(intent)
            } else {
                Log.w(TAG, "Location permission denied")
                Toast.makeText(
                    requireContext(),
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }

    companion object {
        private const val TAG = "FavFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}