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
            
            setupUI()
            setupRecyclerView()
            loadSampleRecords()
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
                if (isWorking) {
                    checkOut()
                } else {
                    checkIn()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置 UI 失败", Toast.LENGTH_SHORT).show()
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

    private fun checkIn() {
        try {
            startTime = System.currentTimeMillis()
            isWorking = true
            
            val record = WorkRecord(
                date = getCurrentDate(),
                checkInTime = startTime,
                checkOutTime = null,
                duration = 0
            )
            records.add(0, record)
            binding.historyRecyclerView.adapter?.notifyItemInserted(0)
            
            updateUIForCheckIn(startTime)
            handler.post(timerRunnable)
            Toast.makeText(this, "开始工作，加油！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打卡失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOut() {
        try {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            isWorking = false
            handler.removeCallbacks(timerRunnable)
            
            // 更新历史记录
            if (records.isNotEmpty()) {
                val oldRecord = records[0]
                records[0] = WorkRecord(
                    date = oldRecord.date,
                    checkInTime = oldRecord.checkInTime,
                    checkOutTime = endTime,
                    duration = duration
                )
                binding.historyRecyclerView.adapter?.notifyItemChanged(0)
            }
            
            updateUIForCheckOut(endTime, duration)
            Toast.makeText(this, "辛苦了，休息一下吧！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "结束失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIForCheckIn(time: Long) {
        try {
            binding.checkInTimeText.text = formatTime(time)
            binding.checkOutTimeText.text = "--:--:--"
            binding.statusText.text = "工作中... 再次点击结束"
            binding.durationText.text = "工作时长：00:00:00"
            
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
            binding.durationText.text = "工作时长：${formatDurationHourMinute(duration)}"
            
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
            binding.durationText.text = "工作时长：${formatDurationHourMinute(elapsed)}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSampleRecords() {
        try {
            records.add(WorkRecord("2026-03-10", System.currentTimeMillis() - 86400000, System.currentTimeMillis() - 3600000, 28800000))
            records.add(WorkRecord("2026-03-09", System.currentTimeMillis() - 172800000, System.currentTimeMillis() - 122400000, 32400000))
            binding.historyRecyclerView.adapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetUI() {
        try {
            binding.checkInTimeText.text = "--:--:--"
            binding.checkOutTimeText.text = "--:--:--"
            binding.statusText.text = "点击打卡开始工作"
            binding.durationText.text = "工作时长：00:00:00"
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
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "--:--"
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
