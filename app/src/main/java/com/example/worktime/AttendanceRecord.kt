package com.example.worktime

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,           // 日期 yyyy-MM-dd
    val checkInTime: Long,      // 打卡开始时间戳
    val checkOutTime: Long?,    // 打卡结束时间戳（可为空）
    val duration: Long          // 工作时长（毫秒）
)
