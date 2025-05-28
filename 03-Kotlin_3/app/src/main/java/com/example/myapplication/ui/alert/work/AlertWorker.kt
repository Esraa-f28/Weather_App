package com.example.myapplication.ui.alert.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.R
import com.example.myapplication.model.local.AlarmType
import com.example.myapplication.model.local.LocalDataSource
import com.example.myapplication.model.remote.RemoteDataSource
import com.example.myapplication.model.repo.Repository
import com.example.myapplication.ui.alert.AlarmBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AlertWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            Log.d("AlertWorker", "Worker started for job $id")
            val alarmTypeString = inputData.getString(KEY_ALARM_TYPE)
            val alertId = inputData.getString(KEY_ALERT_ID)
            val fromTimeMillis = inputData.getLong(KEY_FROM_TIME, 0L)
            val toTimeMillis = inputData.getLong(KEY_TO_TIME, 0L)

            val alarmType = try {
                alarmTypeString?.let { AlarmType.valueOf(it) } ?: AlarmType.NOTIFICATION
            } catch (e: IllegalArgumentException) {
                Log.e("AlertWorker", "Invalid alarmType: $alarmTypeString")
                AlarmType.NOTIFICATION
            }

            val repository = Repository.getInstance(RemoteDataSource(), LocalDataSource.getInstance(applicationContext))
            if (alertId != null) {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        try {
                            repository.updateAlertStatus(alertId, false)
                            Log.d("AlertWorker", "Marked alert $alertId as inactive")
                        } catch (e: Exception) {
                            Log.e("AlertWorker", "Failed to update alert status: ${e.message}")
                        }
                    }
                }
            }

            val sharedPreferences = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)

            if (notificationsEnabled) {
                showNotification(alarmType, fromTimeMillis, toTimeMillis, alertId, repository)

                if (alarmType == AlarmType.DEFAULT_ALARM) {
                    Log.d("AlertWorker", "Triggering alarm for alertId=$alertId")
                    val intent = Intent(AlarmBroadcastReceiver.ACTION_START_ALARM).apply {
                        setClass(applicationContext, AlarmBroadcastReceiver::class.java)
                    }
                    applicationContext.sendBroadcast(intent)
                }
            } else {
                Log.d("AlertWorker", "Notification disabled in preferences. Skipping notification.")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("AlertWorker", "Worker failed: ${e.message}", e)
            return Result.failure()
        }
    }

    private fun showNotification(
        alarmType: AlarmType,
        fromTimeMillis: Long,
        toTimeMillis: Long,
        alertId: String?,
        repository: Repository
    ) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "alert_channel",
                    "Weather Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy h:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val fromTime = if (fromTimeMillis != 0L) dateFormat.format(fromTimeMillis) else "Unknown"
            val toTime = if (toTimeMillis != 0L) dateFormat.format(toTimeMillis) else "Unknown"

            val sharedPreferences = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val latitude = sharedPreferences.getFloat("pref_latitude", 0.0f).toDouble()
            val longitude = sharedPreferences.getFloat("pref_longitude", 0.0f).toDouble()
            val apiKey = "36f28ef1ca3386a0cd3bff9801d97e53"

            Log.d("AlertWorker", "Fetched latitude: $latitude, longitude: $longitude")

            var weatherDescription = "Unable to fetch weather data"
            if (latitude != 0.0 || longitude != 0.0) {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        try {
                            val result = repository.getCurrentWeather(latitude, longitude, apiKey)
                            result.getOrNull()?.let { weatherResponse ->
                                Log.d("AlertWorker", "Weather response: $weatherResponse")
                                weatherDescription = weatherResponse.weather?.firstOrNull()?.description ?: "No description available"
                            } ?: run { Log.e("AlertWorker", "Weather response is null") }
                        } catch (e: Exception) {
                            Log.e("AlertWorker", "Error fetching weather data: ${e.message}", e)
                        }
                    }
                }
            } else {
                Log.w("AlertWorker", "Invalid coordinates, skipping weather fetch")
            }

            val notification = NotificationCompat.Builder(applicationContext, "alert_channel")
                .setSmallIcon(R.drawable.bell)
                .setContentTitle("Weather Alert")
                .setContentText("($weatherDescription)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationId = alertId?.hashCode() ?: UUID.randomUUID().hashCode()
            notificationManager.notify(notificationId, notification)
            Log.d("AlertWorker", "Notification shown for alertId=$alertId with weather: $weatherDescription")
        } catch (e: Exception) {
            Log.e("AlertWorker", "Error showing notification: ${e.message}", e)
        }
    }

    companion object {
        const val KEY_ALARM_TYPE = "alarm_type"
        const val KEY_ALERT_ID = "alert_id"
        const val KEY_FROM_TIME = "from_time"
        const val KEY_TO_TIME = "to_time"
    }
}