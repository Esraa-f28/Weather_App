// ui/alert/AlarmBroadcastReceiver.kt
package com.example.myapplication.ui.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.util.AlarmManager

class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                ACTION_START_ALARM -> {
                    Log.d("AlarmReceiver", "Received start alarm intent")
                    AlarmManager.startAlarm(context)
                }
                ACTION_STOP_ALARM -> {
                    Log.d("AlarmReceiver", "Received stop alarm intent")
                    AlarmManager.stopAlarm()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error handling broadcast: ${e.message}", e)
        }
    }

    companion object {
        const val ACTION_START_ALARM = "com.example.myapplication.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.myapplication.STOP_ALARM"
    }
}