package com.quin.weather

import android.os.Bundle
import android.util.Log.d
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.quin.weather.databinding.FragmentWeatherBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentWeatherBinding? = null
    private val binding
        get() = _binding!!
    private val apiKey = "9719476c7c0eeca1283d36843c23645c"
    private var lastLatLng: LatLng? = null
    private lateinit var googleMap: GoogleMap
    private var cityAdapter: CityAdapter? = null
    private var mapReady = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        setUpRecycler()
        initClickListener()
        binding.etSearchCity.addTextChangedListener { text ->
            binding.rvSearchResults.isVisible = true
            binding.cvWeatherResponse.isVisible = false
            binding.cvMap.isVisible = false
            if (!text.isNullOrEmpty()) {
                searchCity(text.toString())
            }
        }
    }

    private fun initClickListener() {
        binding.btnSave.setOnClickListener {
            val cityName = binding.tvCityName.text.toString()
            val tempText = binding.tvDegree.text.toString().replace("°C", "").trim()
            val condition = binding.tvWeather.text.toString()
            val iconUrl = binding.ivWeather.tag?.toString() ?: ""
            val dateTime = binding.tvDateTime.text.toString()
            val lat = lastLatLng?.latitude ?: 0.0
            val lon = lastLatLng?.longitude ?: 0.0

            val weatherEntity = WeatherEntity(
                city = cityName,
                temp = tempText.toDoubleOrNull() ?: 0.0,
                condition = condition,
                icon = iconUrl,
                dateTime = dateTime,
                lat = lat,
                lon = lon
            )

            CoroutineScope(Dispatchers.IO).launch {
                val dao = Utilities.getDatabase(requireContext()).weatherDao()
                val existing = dao.getWeatherByCity(cityName)
                if (existing == null) {
                    dao.insertWeather(weatherEntity)
                    withContext(Dispatchers.Main) {
                        findNavController().navigateUp()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "City already saved!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun setUpRecycler() {
        cityAdapter = CityAdapter {
            d("WeatherResponses", "$it")
            binding.rvSearchResults.visibility = View.GONE
            binding.cvWeatherResponse.visibility = View.VISIBLE
            binding.cvMap.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
            fetchWeather(it.name)
        }
        binding.rvSearchResults.adapter = cityAdapter
    }

    private fun fetchWeather(city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Utilities.api.getWeather(city, apiKey)
                withContext(Dispatchers.Main) {
                    val date = Date(response.dt * 1000L)
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val formattedDate = sdf.format(date)
                    val weatherInfo = """
                        City: ${response.name}
                        Temp: ${response.main.temp}°C
                        Condition: ${response.weather[0].description}
                        ${response.weather[0].icon}
                        Date & Time: $formattedDate
                    """.trimIndent()
                    val lat = response.coord.lat
                    val lon = response.coord.lon
                    d("WeatherResponses", "$weatherInfo")
                    withContext(Dispatchers.Main) {
                        binding.tvCityName.text = "${response.name}"
                        binding.tvDateTime.text = "$formattedDate"
                        val iconCode = response.weather[0].icon
                        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                        binding.ivWeather.load(iconUrl)
                        binding.ivWeather.tag = iconUrl
                        binding.tvDegree.text = "${response.main.temp}°C"
                        binding.tvLatLong.text = "Lat:$lat Lon:$lon"
                        binding.tvWeather.text = "${response.weather[0].description}"
                        lastLatLng = LatLng(lat, lon)
                        if (mapReady) updateMapMarker(lastLatLng!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun searchCity(cityName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Utilities.api.searchCity(cityName, 10, apiKey)
                withContext(Dispatchers.Main) {
                    cityAdapter?.submitList(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true
        lastLatLng?.let { latLng ->
            updateMapMarker(latLng)
        }
    }

    private fun updateMapMarker(latLng: LatLng) {
        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions().position(latLng).title(binding.tvCityName.text.toString())
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

}