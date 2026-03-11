package com.example.worktime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
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
        startTime = System.currentTimeMillis()
        isWorking = true
        updateUIForCheckIn(startTime)
        handler.post(timerRunnable)
    }

    private fun checkOut() {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        isWorking = false
        handler.removeCallbacks(timerRunnable)
        
        val record = WorkRecord(
            date = getCurrentDate(),
            startTime = formatTime(startTime),
            endTime = formatTime(endTime),
            duration = formatDuration(duration)
        )
        records.add(0, record)
        binding.historyRecyclerView.adapter?.notifyItemInserted(0)
        
        updateUIForCheckOut(endTime, duration)
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

    private fun loadSampleRecords() {
        records.add(WorkRecord("2026-03-10", "09:00", "18:00", "9 小时 0 分钟"))
        records.add(WorkRecord("2026-03-09", "09:15", "18:30", "9 小时 15 分钟"))
        binding.historyRecyclerView.adapter?.notifyDataSetChanged()
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
