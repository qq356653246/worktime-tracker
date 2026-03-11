package com.example.worktime

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.worktime.databinding.ItemRecordBinding

class RecordsAdapter(private val records: List<WorkRecord>) : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: WorkRecord) {
            binding.dateText.text = record.date
            binding.timeText.text = "${record.startTime} - ${record.endTime}"
            binding.durationText.text = record.duration
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount() = records.size
}
