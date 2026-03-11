package com.example.worktime

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AttendanceDatabase
    private lateinit var dao: AttendanceDao
    
    private var isWorking = false
    private var currentRecordId: Long = 0
    private var startTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val records = mutableListOf<AttendanceRecord>()
    
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

        // 初始化数据库
        database = AttendanceDatabase.getDatabase(this)
        dao = database.attendanceDao()

        setupUI()
        setupRecyclerView()
        loadTodayRecord()
        observeAllRecords()
    }

    private fun setupUI() {
        // 显示今天日期
        val sdf = SimpleDateFormat("yyyy 年 MM 月 dd 日 EEEE", Locale.CHINA)
        binding.dateText.text = sdf.format(Date())

        // 打卡按钮点击事件
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
        lifecycleScope.launch {
            startTime = System.currentTimeMillis()
            val date = getCurrentDate()
            
            // 创建新记录
            val record = AttendanceRecord(
                date = date,
                checkInTime = startTime,
                checkOutTime = null,
                duration = 0
            )
            
            currentRecordId = withContext(Dispatchers.IO) {
                dao.insertRecord(record)
            }
            
            isWorking = true
            updateUIForCheckIn(startTime)
            handler.post(timerRunnable)
        }
    }

    private fun checkOut() {
        lifecycleScope.launch {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            isWorking = false
            handler.removeCallbacks(timerRunnable)
            
            // 更新记录
            withContext(Dispatchers.IO) {
                dao.updateRecord(
                    AttendanceRecord(
                        id = currentRecordId,
                        date = getCurrentDate(),
                        checkInTime = startTime,
                        checkOutTime = endTime,
                        duration = duration
                    )
                )
            }
            
            updateUIForCheckOut(endTime, duration)
        }
    }

    private fun updateUIForCheckIn(time: Long) {
        val timeStr = formatTime(time)
        binding.checkInTimeText.text = timeStr
        binding.checkOutTimeText.text = "--:--:--"
        binding.statusText.text = "工作中... 再次点击结束"
        binding.durationText.text = "工作时长：00:00:00"
        
        // 改变按钮颜色为结束打卡
        binding.checkButton.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(
                getColor(R.color.check_button_end)
            )
    }

    private fun updateUIForCheckOut(time: Long, duration: Long) {
        val timeStr = formatTime(time)
        binding.checkOutTimeText.text = timeStr
        binding.statusText.text = "今日打卡已完成"
        binding.durationText.text = "工作时长：${formatDuration(duration)}"
        
        // 改变按钮颜色为开始打卡
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
        lifecycleScope.launch {
            val today = getCurrentDate()
            val unfinishedRecord = withContext(Dispatchers.IO) {
                dao.getUnfinishedRecord(today)
            }
            
            if (unfinishedRecord != null) {
                // 恢复未完成记录
                currentRecordId = unfinishedRecord.id
                startTime = unfinishedRecord.checkInTime
                isWorking = true
                updateUIForCheckIn(startTime)
                handler.post(timerRunnable)
            } else {
                // 没有未完成记录
                resetUI()
            }
        }
    }

    private fun observeAllRecords() {
        lifecycleScope.launch {
            dao.getAllRecords().collectLatest { list ->
                records.clear()
                records.addAll(list)
                binding.historyRecyclerView.adapter?.notifyDataSetChanged()
            }
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

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
