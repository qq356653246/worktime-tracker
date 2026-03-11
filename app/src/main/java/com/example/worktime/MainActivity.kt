package com.example.worktime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.worktime.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: WorkTimeDatabase
    private lateinit var dao: WorkTimeDao
    
    private var isWorking = false
    private var startTime: Long = 0
    private var currentRecordId: Long = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private val records = mutableListOf<WorkRecord>()
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
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 初始化数据库（在后台线程）
            executor.execute {
                database = WorkTimeDatabase.getDatabase(this@MainActivity)
                dao = database.workTimeDao()
                
                runOnUiThread {
                    setupUI()
                    setupRecyclerView()
                    loadTodayRecord()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败：${e.message}", Toast.LENGTH_LONG).show()
        }
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
        
        executor.execute {
            try {
                // 检查今天是否已经有未完成的打卡记录
                val unfinishedRecord = dao.getUnfinishedRecord(today)
                
                if (unfinishedRecord != null) {
                    // 已有未完成记录，恢复状态
                    currentRecordId = unfinishedRecord.id
                    startTime = unfinishedRecord.checkInTime
                    isWorking = true
                    
                    runOnUiThread {
                        updateUIForCheckIn(startTime)
                        handler.post(timerRunnable)
                        Toast.makeText(this@MainActivity, "继续工作", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 创建新记录
                    val record = WorkRecord(
                        date = today,
                        checkInTime = now,
                        checkOutTime = null,
                        duration = 0
                    )
                    
                    currentRecordId = dao.insertRecord(record)
                    startTime = now
                    isWorking = true
                    
                    runOnUiThread {
                        updateUIForCheckIn(startTime)
                        handler.post(timerRunnable)
                        Toast.makeText(this@MainActivity, "开始工作，加油！", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "打卡失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkOut() {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        isWorking = false
        handler.removeCallbacks(timerRunnable)
        
        executor.execute {
            try {
                // 更新记录
                dao.updateRecord(
                    WorkRecord(
                        id = currentRecordId,
                        date = getCurrentDate(),
                        checkInTime = startTime,
                        checkOutTime = endTime,
                        duration = duration
                    )
                )
                
                // 重新加载历史记录
                val allRecords = dao.getAllRecords()
                runOnUiThread {
                    records.clear()
                    records.addAll(allRecords)
                    binding.historyRecyclerView.adapter?.notifyDataSetChanged()
                }
                
                runOnUiThread {
                    updateUIForCheckOut(endTime, duration)
                    Toast.makeText(this@MainActivity, "辛苦了，休息一下吧！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "结束失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        binding.durationText.text = "工作时长：${formatDurationHourMinute(duration)}"
        
        binding.checkButton.backgroundTintList = 
            android.content.res.ColorStateList.valueOf(
                getColor(R.color.check_button_start)
            )
    }

    private fun updateTimer() {
        val elapsed = System.currentTimeMillis() - startTime
        binding.durationText.text = "工作时长：${formatDurationHourMinute(elapsed)}"
    }

    private fun loadTodayRecord() {
        executor.execute {
            try {
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
                        resetUI()
                    }
                    
                    // 加载所有历史记录
                    val allRecords = dao.getAllRecords()
                    records.clear()
                    records.addAll(allRecords)
                    binding.historyRecyclerView.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    resetUI()
                    Toast.makeText(this@MainActivity, "加载记录失败", Toast.LENGTH_SHORT).show()
                }
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
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
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
    }
}
