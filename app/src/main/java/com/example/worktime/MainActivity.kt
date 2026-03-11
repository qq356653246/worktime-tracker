package com.example.worktime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: android.content.SharedPreferences
    
    private var isWorking = false
    private var startTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val records = mutableListOf<WorkRecord>()
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isWorking) {
                updateTimer()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            prefs = getSharedPreferences("worktime_data", MODE_PRIVATE)
            
            setupUI()
            setupRecyclerView()
            loadSavedRecords()
            updateMonthStats()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败：${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        try {
            val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 EEEE", Locale.CHINA)
            binding.dateText.text = sdf.format(Date())

            binding.checkButton.setOnClickListener {
                handleCheckClick()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCheckClick() {
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        val todayIndex = records.indexOfFirst { it.date == today }
        
        if (todayIndex == -1) {
            // 今天还没有打卡记录 - 开始打卡
            checkIn(today, now)
        } else {
            val todayRecord = records[todayIndex]
            if (todayRecord.checkOutTime == null) {
                // 已有开始时间，没有结束时间 - 结束打卡
                checkOut(todayIndex, now)
            } else {
                // 今天已完成打卡 - 更新结束时间为最新时间
                checkOut(todayIndex, now)
            }
        }
    }

    private fun checkIn(date: String, time: Long) {
        try {
            startTime = time
            isWorking = true
            
            val record = WorkRecord(
                date = date,
                checkInTime = time,
                checkOutTime = null,
                duration = 0
            )
            records.add(0, record)
            binding.historyRecyclerView.adapter?.notifyItemInserted(0)
            
            saveRecords()
            
            updateUIForCheckIn(time)
            handler.post(timerRunnable)
            Toast.makeText(this, "开始工作，加油！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkOut(index: Int, time: Long) {
        try {
            val oldRecord = records[index]
            val duration = time - oldRecord.checkInTime
            
            isWorking = false
            handler.removeCallbacks(timerRunnable)
            
            // 更新记录 - 无论是否已有结束时间，都更新为最新时间
            records[index] = WorkRecord(
                date = oldRecord.date,
                checkInTime = oldRecord.checkInTime,
                checkOutTime = time,  // 总是使用最新的结束时间
                duration = duration
            )
            binding.historyRecyclerView.adapter?.notifyItemChanged(index)
            
            saveRecords()
            updateMonthStats()
            
            updateUIForCheckOut(time, duration)
            Toast.makeText(this, "打卡成功！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUIForCheckIn(time: Long) {
        try {
            binding.checkInTimeText.text = formatTime(time)
            binding.checkOutTimeText.text = "--:--:--"
            binding.statusText.text = "工作中... 再次点击结束"
            binding.durationText.text = "工作时长：0 小时 0 分钟 0 秒"
            
            binding.checkButton.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.check_button_end)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUIForCheckOut(time: Long, duration: Long) {
        try {
            binding.checkOutTimeText.text = formatTime(time)
            binding.statusText.text = "今日打卡已完成"
            binding.durationText.text = "工作时长：${formatDuration(duration)}"
            
            binding.checkButton.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.check_button_start)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTimer() {
        try {
            val elapsed = System.currentTimeMillis() - startTime
            binding.durationText.text = "工作时长：${formatDuration(elapsed)}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveRecords() {
        try {
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(records)
            editor.putString("records", json)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedRecords() {
        try {
            val json = prefs.getString("records", null)
            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<WorkRecord>>() {}.type
                val savedRecords: MutableList<WorkRecord> = gson.fromJson(json, type)
                records.clear()
                records.addAll(savedRecords)
                binding.historyRecyclerView.adapter?.notifyDataSetChanged()
                
                val today = getCurrentDate()
                val todayRecord = records.find { it.date == today && it.checkOutTime == null }
                if (todayRecord != null) {
                    isWorking = true
                    startTime = todayRecord.checkInTime
                    updateUIForCheckIn(startTime)
                    handler.post(timerRunnable)
                } else {
                    // 检查今天是否有已完成的记录
                    val todayCompleted = records.find { it.date == today && it.checkOutTime != null }
                    if (todayCompleted != null) {
                        updateUIForCheckOut(todayCompleted.checkOutTime!!, todayCompleted.duration)
                    } else {
                        resetUI()
                    }
                }
            } else {
                resetUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            resetUI()
        }
    }

    private fun updateMonthStats() {
        try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val monthEnd = calendar.timeInMillis
            
            val monthRecords = records.filter { record ->
                val recordTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.date)?.time ?: 0
                recordTime in monthStart..monthEnd && record.checkOutTime != null
            }
            
            val totalDays = monthRecords.size
            val totalHours = monthRecords.sumOf { it.duration } / (1000 * 60 * 60)
            
            binding.monthStatsText.text = "本月已工作 $totalDays 天，总计 $totalHours 小时"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        try {
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.historyRecyclerView.adapter = HistoryAdapter(records)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetUI() {
        try {
            binding.checkInTimeText.text = "--:--:--"
            binding.checkOutTimeText.text = "--:--:--"
            binding.statusText.text = "点击打卡开始工作"
            binding.durationText.text = "工作时长：0 小时 0 分钟 0 秒"
            binding.checkButton.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.check_button_start)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatTime(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "--:--:--"
        }
    }

    private fun formatDuration(ms: Long): String {
        return try {
            val hours = TimeUnit.MILLISECONDS.toHours(ms)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
            "${hours}小时${minutes}分钟${seconds}秒"
        } catch (e: Exception) {
            "0 小时 0 分钟 0 秒"
        }
    }

    private fun getCurrentDate(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date())
        } catch (e: Exception) {
            "2026-01-01"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        if (isWorking) {
            saveRecords()
        }
    }
}
