package com.quin.weather


data class WeatherResponse(
    val dt: Long, // timestamp
    val name: String, // city name
    val main: Main,
    val weather: List<Weather>,
    val coord: Coord
)

data class Main(
    val temp: Double,
    val humidity: Int
)

data class Weather(
    val description: String,
    val icon: String
)

data class Coord(
    val lon: Double,
    val lat: Double
)
