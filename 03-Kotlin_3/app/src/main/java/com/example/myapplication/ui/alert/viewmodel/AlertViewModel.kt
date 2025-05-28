package com.example.myapplication.ui.alert.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.myapplication.model.local.AlarmType
import com.example.myapplication.model.local.Alert
import com.example.myapplication.model.repo.Repository
import com.example.myapplication.ui.alert.AlarmBroadcastReceiver
import com.example.myapplication.ui.alert.work.AlertWorker
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import androidx.work.await

class AlertViewModel(private val repository: Repository, private val context: Context) : ViewModel() {

    private val _alerts = MutableLiveData<List<Alert>>()
    val alerts: LiveData<List<Alert>> = _alerts

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            try {
                _alerts.postValue(repository.getAlerts())
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error loading alerts: ${e.message}")
                _alerts.postValue(emptyList())
            }
        }
    }

    fun addAlert(durationHours: Int, alarmType: AlarmType, fromTimeMillis: Long, toTimeMillis: Long) {
        viewModelScope.launch {
            try {
                val alert = Alert(
                    id = UUID.randomUUID().toString(),
                    durationHours = durationHours,
                    alarmType = alarmType,
                    isActive = true,
                    fromTimeMillis = fromTimeMillis,
                    toTimeMillis = toTimeMillis
                )
                Log.d("AlertViewModel", "Adding alert with alarmType=${alert.alarmType}")
                repository.addAlert(alert)
                _alerts.value = repository.getAlerts() ?: emptyList() // Force refresh
                scheduleAlert(alert)
                loadAlerts()
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error adding alert: ${e.message}")
            }
        }
    }

    fun snoozeAlert(alertId: String) {
        viewModelScope.launch {
            try {
                val alert = repository.getAlerts().find { it.id == alertId }
                if (alert == null) {
                    Log.w("AlertViewModel", "Alert $alertId not found for snooze")
                    return@launch
                }
                // Stop the current alarm
                val stopIntent = Intent(AlarmBroadcastReceiver.ACTION_STOP_ALARM).apply {
                    setPackage(context.packageName)
                    putExtra("alertId", alertId)
                }
                context.sendBroadcast(stopIntent)
                Log.d("AlertViewModel", "Stop broadcast sent for snooze of alert $alertId")

                // Reschedule the alert for 5 minutes from now
                val snoozeDurationMillis = 5 * 60 * 1000L // 5 minutes
                val newToTimeMillis = System.currentTimeMillis() + snoozeDurationMillis
                val newAlert = alert.copy(
                    fromTimeMillis = System.currentTimeMillis(),
                    toTimeMillis = newToTimeMillis,
                    durationHours = 0 // Duration is not relevant for snoozed alerts
                )
                repository.updateAlert(newAlert)
                scheduleAlert(newAlert)
                Log.d("AlertViewModel", "Snoozed alert $alertId for 5 minutes")
                loadAlerts()
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error snoozing alert $alertId: ${e.message}", e)
            }
        }
    }

    fun stopAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.updateAlertStatus(alertId, false)
                val workInfo = WorkManager.getInstance(context).getWorkInfosForUniqueWork(alertId).get()
                Log.d("AlertViewModel", "Work state for $alertId: ${workInfo.joinToString { it.state.name }}")
                WorkManager.getInstance(context).cancelUniqueWork(alertId).await()
                val stopIntent = Intent(AlarmBroadcastReceiver.ACTION_STOP_ALARM).apply {
                    setPackage(context.packageName) // Scope the broadcast to this app
                    putExtra("alertId", alertId) // Optional: Add alert ID for debugging
                }
                Log.d("AlertViewModel", "Sending stop broadcast for alert $alertId with action ${stopIntent.action}, package ${context.packageName}")
                context.sendBroadcast(stopIntent)
                Log.d("AlertViewModel", "Stop broadcast sent for alert $alertId")
                loadAlerts()
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error stopping alert $alertId: ${e.message}", e)
            }
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAlert(alertId)
                WorkManager.getInstance(context).cancelUniqueWork(alertId)
                val stopIntent = Intent(AlarmBroadcastReceiver.ACTION_STOP_ALARM).apply {
                    setPackage(context.packageName) // Scope the broadcast to this app
                    putExtra("alertId", alertId) // Optional: Add alert ID for debugging
                }
                context.sendBroadcast(stopIntent)
                Log.d("AlertViewModel", "Deleting alert $alertId and sending stop alarm broadcast")
                loadAlerts()
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error deleting alert $alertId: ${e.message}")
            }
        }
    }

    private fun scheduleAlert(alert: Alert) {
        if (!alert.isActive || alert.toTimeMillis <= System.currentTimeMillis()) {
            Log.d("AlertViewModel", "Not scheduling alert ${alert.id}: inactive or time passed")
            return
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("AlertViewModel", "Cannot schedule alert ${alert.id}: Notification permission not granted")
            return
        }

        val data = Data.Builder()
            .putString(AlertWorker.KEY_ALARM_TYPE, alert.alarmType.name)
            .putString(AlertWorker.KEY_ALERT_ID, alert.id)
            .putLong(AlertWorker.KEY_FROM_TIME, alert.fromTimeMillis)
            .putLong(AlertWorker.KEY_TO_TIME, alert.toTimeMillis)
            .build()

        val currentTime = System.currentTimeMillis()
        val delayMillis = (alert.toTimeMillis - currentTime).coerceAtLeast(0)
        Log.d(
            "AlertViewModel",
            "Scheduling alert ${alert.id} with alarmType=${alert.alarmType.name}, " +
                    "toTime=${alert.toTimeMillis}, delay=${delayMillis}ms"
        )

        val workRequest = OneTimeWorkRequest.Builder(AlertWorker::class.java)
            .setInputData(data)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(alert.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(alert.id, ExistingWorkPolicy.REPLACE, workRequest)
    }
}