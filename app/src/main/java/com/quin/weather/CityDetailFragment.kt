package com.quin.weather

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.quin.weather.databinding.FragmentCityDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CityDetailFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCityDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private var mapReady = false
    private var lastLatLng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup MapView
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        val cityName = arguments?.getString("cityName") ?: return

        fetchWeatherFromDb(cityName)
    }

    private fun fetchWeatherFromDb(cityName: String) {
        val weatherDao = Utilities.getDatabase(requireContext()).weatherDao()
        viewLifecycleOwner.lifecycleScope.launch {
            val entity = withContext(Dispatchers.IO) {
                weatherDao.getWeatherByCity(cityName)
            }
            entity?.let { updateUI(it) }
        }
    }

    private fun updateUI(entity: WeatherEntity) {
        binding.tvCityName.text = entity.city
        binding.tvDegree.text = "${entity.temp}°C"
        binding.tvWeather.text = entity.condition
        binding.tvDateTime.text = entity.dateTime
        binding.tvLatLong.text = "Lat: ${entity.lat}, Lon: ${entity.lon}"

        binding.ivWeather.load(entity.icon)
        binding.ivWeather.tag = entity.icon

        lastLatLng = LatLng(entity.lat, entity.lon)
        if (mapReady) updateMapMarker(lastLatLng!!)
    }

    private fun updateMapMarker(latLng: LatLng) {
        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions().position(latLng).title(binding.tvCityName.text.toString())
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    // MapView lifecycle
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true
        lastLatLng?.let { updateMapMarker(it) }
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { binding.mapView.onPause(); super.onPause() }
    override fun onDestroyView() {
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }
}
