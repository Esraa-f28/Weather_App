// util/AlarmManager.kt
package com.example.myapplication.util

import android.content.Context
import android.media.MediaPlayer
import android.provider.Settings
import android.util.Log

object AlarmManager {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false

    fun startAlarm(context: Context) {
        if (isPlaying) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
            if (mediaPlayer == null) {
                Log.e("AlarmManager", "MediaPlayer.create failed")
                return
            }
            mediaPlayer?.apply {
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.e("AlarmManager", "MediaPlayer error: what=$what, extra=$extra")
                    stopAlarm()
                    true
                }
                start()
            }
            isPlaying = true
            Log.d("AlarmManager", "Alarm started")
        } catch (e: Exception) {
            Log.e("AlarmManager", "Error starting alarm: ${e.message}", e)
            stopAlarm()
        }
    }

    fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            Log.d("AlarmManager", "Alarm stopped")
        } catch (e: Exception) {
            Log.e("AlarmManager", "Error stopping alarm: ${e.message}", e)
        }
    }
}