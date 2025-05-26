package com.example.myapplication.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert")
data class Alert(
    @PrimaryKey val id: String,
    val durationHours: Int,
    val alarmType: AlarmType,
    val isActive: Boolean,
    val fromTimeMillis: Long,
    val toTimeMillis: Long
)

enum class AlarmType {
    NOTIFICATION,
    DEFAULT_ALARM
}