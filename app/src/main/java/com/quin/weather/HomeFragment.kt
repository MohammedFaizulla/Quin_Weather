package com.quin.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.quin.weather.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val apiKey = "9719476c7c0eeca1283d36843c23645c"
    private var lastLatLng: LatLng? = null
    private lateinit var googleMap: GoogleMap
    private var mapReady = false
    private var savedCityAdapter: SavedCityAdapter? = null

    // ------------------- Permission Request -------------------
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.all { it.value }
            if (allGranted) {
                // ✅ start service only after all permissions granted
                startWeatherService()
                getCurrentLocationWeather()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location and Notification permissions are required",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
        }

    private fun checkPermissions(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true

        val allGranted = locationGranted && notificationGranted

        if (!allGranted) {
            val permissions = mutableListOf<String>()
            if (!locationGranted) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!notificationGranted) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // ✅ Ask for missing permissions
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        return allGranted
    }

    private fun setRecyclerView() {
        savedCityAdapter = SavedCityAdapter { city ->
            val bundle = Bundle()
            bundle.putString("cityName", city.cityName)
            findNavController().navigate(R.id.action_homeFragment_to_cityDetailFragment, bundle)
        }
        binding.rvCityWeather.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCityWeather.adapter = savedCityAdapter
    }

    private fun getSavedCity() {
        val weatherDao = Utilities.getDatabase(requireContext().applicationContext).weatherDao()
        viewLifecycleOwner.lifecycleScope.launch {
            weatherDao.getAllWeather().collect { weatherList ->
                if (weatherList.isNotEmpty()) {
                    binding.tvAddCity.visibility = View.GONE
                    binding.rvCityWeather.visibility = View.VISIBLE
                    val savedCityList = weatherList.map { entity ->
                        SavedCity(
                            cityName = entity.city,
                            degree = "${entity.temp}°C"
                        )
                    }
                    savedCityAdapter?.submitList(savedCityList)
                } else {
                    binding.tvAddCity.visibility = View.VISIBLE
                    binding.rvCityWeather.visibility = View.GONE
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocationWeather() {
        if (!checkPermissions()) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchWeatherByCoordinates(location.latitude, location.longitude)
            } else {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).setMaxUpdates(1).build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val freshLocation = result.lastLocation
                            if (freshLocation != null) {
                                fetchWeatherByCoordinates(freshLocation.latitude, freshLocation.longitude)
                            } else {
                                Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    },
                    null
                )
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchWeatherByCoordinates(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Utilities.api.getWeatherByCoordinates(lat, lon, apiKey)
                withContext(Dispatchers.Main) {
                    val date = Date(response.dt * 1000L)
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val formattedDate = sdf.format(date)

                    binding.tvCityName.text = response.name
                    binding.tvDateTime.text = formattedDate
                    binding.tvDegree.text = "${response.main.temp}°C"
                    binding.tvWeather.text = response.weather[0].description
                    lastLatLng = LatLng(lat, lon)

                    val iconUrl = "https://openweathermap.org/img/wn/${response.weather[0].icon}@2x.png"
                    binding.ivWeather.load(iconUrl)
                    binding.ivWeather.tag = iconUrl

                    if (mapReady) updateMapMarker(lastLatLng!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMapMarker(latLng: LatLng) {
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng).title(binding.tvCityName.text.toString()))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        setRecyclerView()
        getSavedCity()
        // ✅ Check permissions on launch
        if (checkPermissions()) {
            startWeatherService()
            getCurrentLocationWeather()
        }
        initClickListener()
    }

    private fun initClickListener() {
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_weatherFragment)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true
        lastLatLng?.let { updateMapMarker(it) }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun startWeatherService() {
        val intent = Intent(requireContext(), WeatherForegroundService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }
}
