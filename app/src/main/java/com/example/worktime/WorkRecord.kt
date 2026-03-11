package com.example.worktime

import java.io.Serializable

data class WorkRecord(
    val date: String,              // 日期 yyyy-MM-dd
    val checkInTime: Long,         // 开始打卡时间戳
    val checkOutTime: Long?,       // 结束打卡时间戳（可为空）
    val duration: Long             // 工作时长（毫秒）
) : Serializable
