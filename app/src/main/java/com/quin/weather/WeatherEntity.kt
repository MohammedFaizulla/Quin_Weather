package com.quin.weather

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "weather_table", indices = [Index(value = ["city"], unique = true)])
data class WeatherEntity(
    val city: String,
    val temp: Double,
    val condition: String,
    val icon: String,
    val dateTime: String,
    val lat: Double,
    val lon: Double,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)