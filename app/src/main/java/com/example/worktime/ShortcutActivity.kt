package com.example.worktime

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * 桌面快捷方式活动
 * 点击桌面快捷方式时直接执行打卡操作
 */
class ShortcutActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            prefs = getSharedPreferences("worktime_data", Context.MODE_PRIVATE)
            performQuickCheck()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打卡失败：${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun performQuickCheck() {
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        
        // 加载记录
        val records = loadRecords()
        val todayIndex = records.indexOfFirst { it.date == today }
        
        if (todayIndex == -1) {
            // 今天还没有打卡，执行签到
            checkIn(today, now, records)
        } else {
            // 今天已经打过卡，检查是否已签退
            val todayRecord = records[todayIndex]
            if (todayRecord.checkOutTime == null) {
                // 已签到但未签退，执行签退
                checkOut(todayIndex, now, records)
            } else {
                // 今天已完成打卡
                Toast.makeText(this, "今日打卡已完成", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun checkIn(date: String, time: Long, records: MutableList<WorkRecord>) {
        val record = WorkRecord(
            date = date,
            checkInTime = time,
            checkOutTime = null,
            duration = 0
        )
        records.add(0, record)
        saveRecords(records)
        
        Toast.makeText(this, "✅ 开始打卡成功", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun checkOut(index: Int, time: Long, records: MutableList<WorkRecord>) {
        val oldRecord = records[index]
        val rawDuration = time - oldRecord.checkInTime
        
        // 计算休息时间
        val breakTimes = loadBreakTimes()
        var totalBreakMs = 0L
        val calendar = Calendar.getInstance()
        
        for (breakTime in breakTimes) {
            val (breakStartHour, breakStartMin) = breakTime.startTime.split(":").map { it.toInt() }
            val (breakEndHour, breakEndMin) = breakTime.endTime.split(":").map { it.toInt() }
            
            calendar.time = Date(oldRecord.checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakStartHour)
            calendar.set(Calendar.MINUTE, breakStartMin)
            calendar.set(Calendar.SECOND, 0)
            val breakStartMs = calendar.timeInMillis
            
            calendar.time = Date(oldRecord.checkInTime)
            calendar.set(Calendar.HOUR_OF_DAY, breakEndHour)
            calendar.set(Calendar.MINUTE, breakEndMin)
            calendar.set(Calendar.SECOND, 0)
            val breakEndMs = calendar.timeInMillis
            
            val actualStart = maxOf(breakStartMs, oldRecord.checkInTime)
            val actualEnd = minOf(breakEndMs, time)
            
            if (actualEnd > actualStart) {
                totalBreakMs += (actualEnd - actualStart)
            }
        }
        
        val netDuration = rawDuration - totalBreakMs
        
        records[index] = WorkRecord(
            date = oldRecord.date,
            checkInTime = oldRecord.checkInTime,
            checkOutTime = time,
            duration = netDuration
        )
        
        saveRecords(records)
        
        val hours = netDuration / (1000 * 60 * 60)
        val minutes = (netDuration / (1000 * 60)) % 60
        
        Toast.makeText(this, "✅ 结束打卡成功\n工作时长：${hours}小时${minutes}分钟", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun loadRecords(): MutableList<WorkRecord> {
        val json = prefs.getString("records", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<MutableList<WorkRecord>>() {}.type
            return gson.fromJson(json, type)
        }
        return mutableListOf()
    }

    private fun saveRecords(records: MutableList<WorkRecord>) {
        val editor = prefs.edit()
        val gson = com.google.gson.Gson()
        val json = gson.toJson(records)
        editor.putString("records", json)
        editor.apply()
    }

    private fun loadBreakTimes(): MutableList<BreakTime> {
        val json = prefs.getString("breakTimes", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<MutableList<BreakTime>>() {}.type
            return gson.fromJson(json, type)
        }
        return mutableListOf(BreakTime("12:00", "13:00"))
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
