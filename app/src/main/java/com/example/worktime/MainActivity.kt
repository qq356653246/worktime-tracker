package com.example.worktime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AttendanceDatabase
    private lateinit var dao: AttendanceDao
    
    private var isWorking = false
    private var currentRecordId: Long = 0
    private var startTime: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val records = mutableListOf<AttendanceRecord>()
    private val executor = Executors.newSingleThreadExecutor()
    
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
        loadAllRecords()
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
        startTime = System.currentTimeMillis()
        val date = getCurrentDate()
        
        // 创建新记录
        val record = AttendanceRecord(
            date = date,
            checkInTime = startTime,
            checkOutTime = null,
            duration = 0
        )
        
        executor.execute {
            currentRecordId = dao.insertRecord(record)
        }
        
        isWorking = true
        updateUIForCheckIn(startTime)
        handler.post(timerRunnable)
    }

    private fun checkOut() {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        isWorking = false
        handler.removeCallbacks(timerRunnable)
        
        // 更新记录
        executor.execute {
            dao.updateRecord(
                AttendanceRecord(
                    id = currentRecordId,
                    date = getCurrentDate(),
                    checkInTime = startTime,
                    checkOutTime = endTime,
                    duration = duration
                )
            )
            loadAllRecords()
        }
        
        updateUIForCheckOut(endTime, duration)
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
        executor.execute {
            val today = getCurrentDate()
            val unfinishedRecord = dao.getUnfinishedRecord(today)
            
            runOnUiThread {
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
    }

    private fun loadAllRecords() {
        executor.execute {
            val allRecords = dao.allRecords
            runOnUiThread {
                records.clear()
                records.addAll(allRecords)
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
