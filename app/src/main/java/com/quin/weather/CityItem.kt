package com.quin.weather

data class CityItem (
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)
typealias GeocodingResponse = List<CityItem>
