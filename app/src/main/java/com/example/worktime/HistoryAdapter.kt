package com.example.worktime

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.worktime.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 历史记录 Adapter - 按天统计
 */
class HistoryAdapter(private val records: List<WorkRecord>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    /**
     * 按日期分组统计记录
     */
    data class DaySummary(
        val date: String,
        val checkInTime: Long,
        val checkOutTime: Long?,
        val totalDuration: Long,
        val checkCount: Int
    )

    private val daySummaries: List<DaySummary> by lazy {
        // 按日期分组
        val grouped = records.groupBy { it.date }
        
        // 计算每天的汇总
        grouped.map { (date, dayRecords) ->
            val completedRecords = dayRecords.filter { it.checkOutTime != null }
            val inProgressRecord = dayRecords.find { it.checkOutTime == null }
            
            val firstCheckIn = dayRecords.minOfOrNull { it.checkInTime } ?: 0L
            val lastCheckOut = completedRecords.maxOfOrNull { it.checkOutTime!! }
            val totalDuration = completedRecords.sumOf { it.duration }
            
            DaySummary(
                date = date,
                checkInTime = firstCheckIn,
                checkOutTime = lastCheckOut ?: inProgressRecord?.checkInTime,
                totalDuration = totalDuration,
                checkCount = dayRecords.size
            )
        }.sortedByDescending { it.date }
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: DaySummary) {
            binding.dateText.text = "${summary.date} (${formatWeekday(summary.date)})"
            
            val checkInStr = formatTime(summary.checkInTime)
            val checkOutStr = summary.checkOutTime?.let { formatTime(it) } ?: "--:--:--"
            
            if (summary.checkCount > 1) {
                binding.timeText.text = "$checkInStr ~ $checkOutStr (${summary.checkCount}次打卡)"
            } else {
                binding.timeText.text = "$checkInStr - $checkOutStr"
            }
            
            binding.durationText.text = if (summary.checkOutTime != null) {
                formatDuration(summary.totalDuration)
            } else {
                "进行中"
            }
            
            if (summary.checkOutTime == null) {
                binding.durationText.setTextColor(
                    binding.root.context.getColor(R.color.check_button_end)
                )
            } else {
                binding.durationText.setTextColor(
                    binding.root.context.getColor(R.color.check_button_start)
                )
            }
        }
        
        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
        
        private fun formatDuration(ms: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(ms)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
            return "${hours}小时${minutes}分钟"
        }
        
        private fun formatWeekday(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr)
                val weekSdf = SimpleDateFormat("EEE", Locale.CHINA)
                weekSdf.format(date!!)
            } catch (e: Exception) {
                ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(daySummaries[position])
    }

    override fun getItemCount() = daySummaries.size
}
