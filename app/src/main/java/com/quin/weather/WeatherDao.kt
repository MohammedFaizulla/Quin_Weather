package com.quin.weather

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather_table")
    fun getAllWeather(): Flow<List<WeatherEntity>>

    @Query("SELECT * FROM weather_table WHERE city = :cityName LIMIT 1")
    suspend fun getWeatherByCity(cityName: String): WeatherEntity?
}