package com.example.worktime

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.EditText
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
    private var currentRecordIndex: Int = -1
    
    private var breakTimes = mutableListOf<BreakTime>()
    
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
            
            loadBreakTimes()
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

    private fun loadBreakTimes() {
        try {
            val json = prefs.getString("breakTimes", null)
            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<BreakTime>>() {}.type
                breakTimes = gson.fromJson(json, type)
            } else {
                // 默认休息时间
                breakTimes = mutableListOf(
                    BreakTime("12:00", "13:00")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            breakTimes = mutableListOf(BreakTime("12:00", "13:00"))
        }
    }

    private fun setupUI() {
        try {
            val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 EEEE", Locale.CHINA)
            binding.dateText.text = sdf.format(Date())

            binding.checkButton.setOnClickListener {
                handleCheckClick()
            }
            
            binding.settingsButton.setOnClickListener {
                showBreakTimeSettings()
            }
            
            binding.shortcutButton.setOnClickListener {
                createDesktopShortcut()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBreakTimeSettings() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("设置休息时间")
            
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.dialog_break_time, null)
            val editText = view.findViewById<EditText>(R.id.breakTimeEditText)
            editText.setText(breakTimes.joinToString(",") { "${it.startTime}-${it.endTime}" })
            
            builder.setView(view)
            builder.setPositiveButton("保存") { dialog, _ ->
                val text = editText.text.toString()
                parseBreakTimes(text)
                saveBreakTimes()
                Toast.makeText(this, "休息时间已保存", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            builder.setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseBreakTimes(text: String) {
        breakTimes.clear()
        val parts = text.split(",", "，")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val times = trimmed.split("-")
                if (times.size == 2) {
                    breakTimes.add(BreakTime(times[0].trim(), times[1].trim()))
                }
            }
        }
        if (breakTimes.isEmpty()) {
            breakTimes.add(BreakTime("12:00", "13:00"))
        }
    }

    private fun saveBreakTimes() {
        try {
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(breakTimes)
            editor.putString("breakTimes", json)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateBreakDuration(): Long {
        try {
            if (currentRecordIndex < 0 || currentRecordIndex >= records.size) return 0
            
            val record = records[currentRecordIndex]
            if (record.checkOutTime == null) return 0
            
            var totalBreakMs = 0L
            val calendar = Calendar.getInstance()
            
            for (breakTime in breakTimes) {
                val (breakStartHour, breakStartMin) = breakTime.startTime.split(":").map { it.toInt() }
                val (breakEndHour, breakEndMin) = breakTime.endTime.split(":").map { it.toInt() }
                
                calendar.time = Date(record.checkInTime)
                calendar.set(Calendar.HOUR_OF_DAY, breakStartHour)
                calendar.set(Calendar.MINUTE, breakStartMin)
                calendar.set(Calendar.SECOND, 0)
                val breakStartMs = calendar.timeInMillis
                
                calendar.time = Date(record.checkInTime)
                calendar.set(Calendar.HOUR_OF_DAY, breakEndHour)
                calendar.set(Calendar.MINUTE, breakEndMin)
                calendar.set(Calendar.SECOND, 0)
                val breakEndMs = calendar.timeInMillis
                
                if (breakStartMs >= record.checkInTime && breakEndMs <= record.checkOutTime!!) {
                    totalBreakMs += (breakEndMs - breakStartMs)
                } else if (breakStartMs < record.checkInTime && breakEndMs > record.checkInTime && breakEndMs <= record.checkOutTime!!) {
                    totalBreakMs += (breakEndMs - record.checkInTime)
                } else if (breakStartMs >= record.checkInTime && breakStartMs < record.checkOutTime!! && breakEndMs > record.checkOutTime!!) {
                    totalBreakMs += (record.checkOutTime!! - breakStartMs)
                } else if (breakStartMs < record.checkInTime && breakEndMs > record.checkOutTime!!) {
                    totalBreakMs += (record.checkOutTime!! - record.checkInTime)
                }
            }
            
            return totalBreakMs
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun handleCheckClick() {
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        val todayIndex = records.indexOfFirst { it.date == today }
        currentRecordIndex = todayIndex
        
        if (todayIndex == -1) {
            checkIn(today, now)
        } else {
            checkOut(todayIndex, now)
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
            currentRecordIndex = 0
            
            saveRecords()
            
            updateUIForCheckIn(time)
            handler.post(timerRunnable)
            
            binding.historyRecyclerView.adapter?.notifyItemInserted(0)
            Toast.makeText(this, "开始工作，加油！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打卡失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOut(index: Int, time: Long) {
        try {
            if (index < 0 || index >= records.size) {
                Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            val oldRecord = records[index]
            val rawDuration = time - oldRecord.checkInTime
            val breakDuration = calculateBreakDurationForNewTime(time, oldRecord.checkInTime)
            val netDuration = rawDuration - breakDuration
            
            isWorking = false
            handler.removeCallbacks(timerRunnable)
            
            records[index] = WorkRecord(
                date = oldRecord.date,
                checkInTime = oldRecord.checkInTime,
                checkOutTime = time,
                duration = netDuration
            )
            
            saveRecords()
            updateMonthStats()
            
            binding.historyRecyclerView.adapter?.notifyItemChanged(index)
            
            updateUIForCheckOut(time, netDuration, breakDuration)
            Toast.makeText(this, "打卡成功！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打卡失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateBreakDurationForNewTime(checkOutTime: Long, checkInTime: Long): Long {
        try {
            var totalBreakMs = 0L
            val calendar = Calendar.getInstance()
            
            for (breakTime in breakTimes) {
                val (breakStartHour, breakStartMin) = breakTime.startTime.split(":").map { it.toInt() }
                val (breakEndHour, breakEndMin) = breakTime.endTime.split(":").map { it.toInt() }
                
                calendar.time = Date(checkInTime)
                calendar.set(Calendar.HOUR_OF_DAY, breakStartHour)
                calendar.set(Calendar.MINUTE, breakStartMin)
                calendar.set(Calendar.SECOND, 0)
                val breakStartMs = calendar.timeInMillis
                
                calendar.time = Date(checkInTime)
                calendar.set(Calendar.HOUR_OF_DAY, breakEndHour)
                calendar.set(Calendar.MINUTE, breakEndMin)
                calendar.set(Calendar.SECOND, 0)
                val breakEndMs = calendar.timeInMillis
                
                val actualStart = maxOf(breakStartMs, checkInTime)
                val actualEnd = minOf(breakEndMs, checkOutTime)
                
                if (actualEnd > actualStart) {
                    totalBreakMs += (actualEnd - actualStart)
                }
            }
            
            return totalBreakMs
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    private fun updateUIForCheckIn(time: Long) {
        try {
            binding.checkInTimeText.text = formatTime(time)
            binding.checkOutTimeText.text = "--:--:--"
            binding.statusText.text = "工作中... 再次点击结束"
            binding.durationText.text = "工作时长：0 小时 0 分钟 0 秒"
            binding.breakDurationText.visibility = android.view.View.GONE
            
            binding.checkButton.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.check_button_end)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUIForCheckOut(time: Long, netDuration: Long, breakDuration: Long) {
        try {
            // 获取当前记录的开始时间并显示
            val checkInTime = if (currentRecordIndex >= 0 && currentRecordIndex < records.size) {
                records[currentRecordIndex].checkInTime
            } else {
                startTime
            }
            
            binding.checkInTimeText.text = formatTime(checkInTime)
            binding.checkOutTimeText.text = formatTime(time)
            binding.statusText.text = "今日打卡已完成"
            binding.durationText.text = "净工作时长：${formatDuration(netDuration)}"
            
            if (breakDuration > 0) {
                binding.breakDurationText.text = "休息时长：${formatDurationHourMinute(breakDuration)}"
                binding.breakDurationText.visibility = android.view.View.VISIBLE
            } else {
                binding.breakDurationText.visibility = android.view.View.GONE
            }
            
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
            val breakDuration = calculateBreakDurationForNewTime(System.currentTimeMillis(), startTime)
            val netElapsed = elapsed - breakDuration
            binding.durationText.text = "净工作时长：${formatDuration(netElapsed)}"
            
            if (breakDuration > 0) {
                binding.breakDurationText.text = "已休息：${formatDurationHourMinute(breakDuration)}"
                binding.breakDurationText.visibility = android.view.View.VISIBLE
            }
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
                val todayIndex = records.indexOfFirst { it.date == today }
                
                if (todayIndex >= 0) {
                    val todayRecord = records[todayIndex]
                    currentRecordIndex = todayIndex
                    
                    if (todayRecord.checkOutTime == null) {
                        isWorking = true
                        startTime = todayRecord.checkInTime
                        updateUIForCheckIn(startTime)
                        handler.post(timerRunnable)
                    } else {
                        val breakDuration = calculateBreakDuration()
                        updateUIForCheckOut(todayRecord.checkOutTime!!, todayRecord.duration, breakDuration)
                    }
                } else {
                    resetUI()
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
            binding.breakDurationText.visibility = android.view.View.GONE
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

    private fun formatDurationHourMinute(ms: Long): String {
        return try {
            val hours = ms / (1000 * 60 * 60)
            val minutes = (ms / (1000 * 60)) % 60
            "${hours}小时${minutes}分钟"
        } catch (e: Exception) {
            "0 小时 0 分钟"
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

    /**
     * 创建桌面快捷方式 (兼容 Android 13+)
     */
    private fun createDesktopShortcut() {
        try {
            // 检查系统是否支持快捷方式 API
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                // 使用现代 ShortcutManager API (Android 7.1+)
                createModernShortcut(shortcutManager)
            } else {
                // 使用传统广播方式 (旧版本 Android)
                createLegacyShortcut()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "创建失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createModernShortcut(shortcutManager: android.content.pm.ShortcutManager) {
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcutIntent = android.content.Intent(this, ShortcutActivity::class.java)
            shortcutIntent.action = android.content.Intent.ACTION_MAIN
            
            val icon = android.graphics.BitmapFactory.decodeResource(
                resources,
                android.R.drawable.ic_menu_my_calendar
            )
            
            val shortcut = android.content.pm.ShortcutInfo.Builder(this, "worktime-shortcut")
                .setShortLabel("一键打卡")
                .setLongLabel("快速打卡")
                .setIcon(android.graphics.drawable.Icon.createWithBitmap(icon))
                .setIntent(shortcutIntent)
                .build()
            
            shortcutManager.requestPinShortcut(shortcut, null)
            Toast.makeText(this, "✅ 请手动将快捷方式添加到桌面", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "❌ 您的启动器不支持快捷方式", Toast.LENGTH_LONG).show()
        }
    }

    private fun createLegacyShortcut() {
        val shortcutIntent = android.content.Intent(this, ShortcutActivity::class.java)
        shortcutIntent.action = android.content.Intent.ACTION_MAIN
        
        val addIntent = android.content.Intent()
        addIntent.putExtra(android.content.Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        addIntent.putExtra(android.content.Intent.EXTRA_SHORTCUT_NAME, "一键打卡")
        addIntent.putExtra(
            android.content.Intent.EXTRA_SHORTCUT_ICON,
            android.graphics.BitmapFactory.decodeResource(
                resources,
                android.R.drawable.ic_menu_my_calendar
            )
        )
        addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        
        sendBroadcast(addIntent)
        Toast.makeText(this, "✅ 快捷方式已创建到桌面", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        if (isWorking) {
            saveRecords()
        }
    }
}
