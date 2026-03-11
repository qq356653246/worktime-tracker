package com.example.worktime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isWorking = false
    private var startTime: Long = 0
    private var todayCheckInTime: Long = 0  // 今天第一次打卡时间
    
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        loadTodayRecord()
        loadSampleRecords()
    }

    private fun setupUI() {
        val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 EEEE", Locale.CHINA)
        binding.dateText.text = sdf.format(Date())

        binding.checkButton.setOnClickListener {
            if (isWorking) {
                checkOut()
            } else {
                checkIn()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = HistoryAdapter(records)
    }

    private fun checkIn() {
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        
        // 检查今天是否已经有打卡记录
        if (todayCheckInTime > 0) {
            // 今天已经打过卡，更新结束时间
            startTime = now
            isWorking = true
            updateUIForCheckIn(todayCheckInTime)  // 显示今天的开始时间
            handler.post(timerRunnable)
            Toast.makeText(this, "继续工作", Toast.LENGTH_SHORT).show()
        } else {
            // 今天第一次打卡
            todayCheckInTime = now
            startTime = now
            isWorking = true
            
            // 添加记录
            val record = WorkRecord(
                date = today,
                startTime = formatTime(todayCheckInTime),
                endTime = "--:--",
                duration = "工作中"
            )
            records.add(0, record)
            binding.historyRecyclerView.adapter?.notifyItemInserted(0)
            
            updateUIForCheckIn(todayCheckInTime)
            handler.post(timerRunnable)
            Toast.makeText(this, "开始工作，加油！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOut() {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        isWorking = false
        handler.removeCallbacks(timerRunnable)
        
        // 更新历史记录中的结束时间
        if (records.isNotEmpty() && records[0].date == getCurrentDate()) {
            val todayRecord = records[0]
            records[0] = WorkRecord(
                date = todayRecord.date,
                startTime = todayRecord.startTime,
                endTime = formatTime(endTime),
                duration = formatDurationHourMinute(duration)
            )
            binding.historyRecyclerView.adapter?.notifyItemChanged(0)
        }
        
        updateUIForCheckOut(endTime, duration)
        Toast.makeText(this, "辛苦了，休息一下吧！", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIForCheckIn(time: Long) {
        binding.checkInTimeText.text = formatTime(time)
        binding.checkOutTimeText.text = "--:--:--"
        binding.statusText.text = "工作中... 再次点击结束"
        binding.durationText.text = "工作时长：00:00:00"
        
        binding.checkButton.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(
                getColor(R.color.check_button_end)
            )
    }

    private fun updateUIForCheckOut(time: Long, duration: Long) {
        binding.checkOutTimeText.text = formatTime(time)
        binding.statusText.text = "今日打卡已完成"
        binding.durationText.text = "工作时长：${formatDuration(duration)}"
        
        binding.checkButton.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(
                getColor(R.color.check_button_start)
            )
    }

    private fun updateTimer() {
        val elapsed = System.currentTimeMillis() - startTime
        binding.durationText.text = "工作时长：${formatDuration(elapsed)}"
    }

    private fun loadTodayRecord() {
        // 检查 SharedPreferences 中是否有今天的打卡记录
        val prefs = getSharedPreferences("worktime", MODE_PRIVATE)
        val today = getCurrentDate()
        val savedDate = prefs.getString("checkInDate", "")
        val savedCheckInTime = prefs.getLong("checkInTime", 0)
        
        if (savedDate == today && savedCheckInTime > 0) {
            todayCheckInTime = savedCheckInTime
            startTime = savedCheckInTime
            isWorking = true
            updateUIForCheckIn(todayCheckInTime)
            handler.post(timerRunnable)
        } else {
            resetUI()
        }
    }

    private fun saveCheckInTime() {
        val prefs = getSharedPreferences("worktime", MODE_PRIVATE)
        prefs.edit().apply {
            putString("checkInDate", getCurrentDate())
            putLong("checkInTime", todayCheckInTime)
            apply()
        }
    }

    private fun loadSampleRecords() {
        // 只在没有历史记录时加载示例数据
        if (records.isEmpty()) {
            records.add(WorkRecord("2026-03-10", "09:00", "18:00", "9 小时 0 分钟"))
            records.add(WorkRecord("2026-03-09", "09:15", "18:30", "9 小时 15 分钟"))
            binding.historyRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun resetUI() {
        binding.checkInTimeText.text = "--:--:--"
        binding.checkOutTimeText.text = "--:--:--"
        binding.statusText.text = "点击打卡开始工作"
        binding.durationText.text = "工作时长：00:00:00"
        binding.checkButton.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(
                getColor(R.color.check_button_start)
            )
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatDurationHourMinute(ms: Long): String {
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms / (1000 * 60)) % 60
        return "${hours}小时${minutes}分钟"
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        if (isWorking && todayCheckInTime > 0) {
            saveCheckInTime()
        }
    }
}
