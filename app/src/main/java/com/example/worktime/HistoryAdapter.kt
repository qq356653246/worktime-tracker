package com.example.worktime

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.worktime.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryAdapter(private val records: List<WorkRecord>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: WorkRecord) {
            binding.dateText.text = record.date
            
            val checkInStr = formatTime(record.checkInTime)
            val checkOutStr = record.checkOutTime?.let { formatTime(it) } ?: "--:--:--"
            binding.timeText.text = "$checkInStr - $checkOutStr"
            
            binding.durationText.text = if (record.checkOutTime != null) {
                formatDuration(record.duration)
            } else {
                "进行中"
            }
            
            if (record.checkOutTime == null) {
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
            "${hours}小时${minutes}分钟"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount() = records.size
}
