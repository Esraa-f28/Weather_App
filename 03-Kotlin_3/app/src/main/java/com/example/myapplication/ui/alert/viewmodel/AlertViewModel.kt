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
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import com.example.myapplication.ui.alert.worker.AlertWorker

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

    fun stopAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.updateAlertStatus(alertId, false)
                WorkManager.getInstance(context).cancelUniqueWork(alertId)
                val stopIntent = Intent(AlarmBroadcastReceiver.ACTION_STOP_ALARM)
                context.sendBroadcast(stopIntent)
                Log.d("AlertViewModel", "Stopping alert $alertId and sending stop alarm broadcast")
                loadAlerts()
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error stopping alert $alertId: ${e.message}")
            }
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAlert(alertId)
                WorkManager.getInstance(context).cancelUniqueWork(alertId)
                val stopIntent = Intent(AlarmBroadcastReceiver.ACTION_STOP_ALARM)
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