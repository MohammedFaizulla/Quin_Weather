package com.quin.weather

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log.d
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WeatherForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: WeatherRepository

    override fun onCreate() {
        super.onCreate()
        d("NotificationService","On create is called")
        repository = WeatherRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, getNotification())

        serviceScope.launch {
            val weatherDao = Utilities.getDatabase(applicationContext).weatherDao()

            while (isActive) {
                try {
                    val weatherList = weatherDao.getAllWeather().first()
                    val cities = weatherList.map { it.city }
                    for (city in cities) {
                        repository.fetchAndSaveWeather(city)
                    }
                    delay(30 * 60 * 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, "weather_channel")
            .setContentTitle("Weather Updates")
            .setContentText("Fetching weather for saved cities...")
            .setSmallIcon(R.drawable.ic_weather)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "weather_channel",
            "Weather Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
