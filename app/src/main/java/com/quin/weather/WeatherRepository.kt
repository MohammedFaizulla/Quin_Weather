package com.quin.weather

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository(private val context: Context) {

    private val weatherDao = Utilities.getDatabase(context).weatherDao()
    private val api = Utilities.api
    private val apiKey = "9719476c7c0eeca1283d36843c23645c"

    suspend fun fetchAndSaveWeather(city: String): WeatherEntity? {
        return try {
            val response = api.getWeather(city, apiKey)

            val date = Date(response.dt * 1000L)
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)

            val iconUrl = "https://openweathermap.org/img/wn/${response.weather[0].icon}@2x.png"

            val weatherEntity = WeatherEntity(
                city = response.name,
                temp = response.main.temp,
                condition = response.weather[0].description,
                icon = iconUrl,
                dateTime = formattedDate,
                lat = response.coord.lat,
                lon = response.coord.lon
            )
            weatherDao.insertWeather(weatherEntity)

            weatherEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
