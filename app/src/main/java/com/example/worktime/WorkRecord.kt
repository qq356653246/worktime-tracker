package com.example.worktime

data class WorkRecord(
    val date: String,
    val checkInTime: Long,
    val checkOutTime: Long?,
    val duration: Long
)
