package com.example.myapplication.ui.alert.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemAlertBinding
import com.example.myapplication.model.local.Alert
import java.text.SimpleDateFormat
import java.util.*

class AlertAdapter(
    private val onStopClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Alert, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    inner class AlertViewHolder(private val binding: ItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Alert, onStopClick: (String) -> Unit, onDeleteClick: (String) -> Unit) {
            try {
                val dateFormat = SimpleDateFormat("dd MMM yyyy h:mm a", Locale.getDefault())
                val fromTime = dateFormat.format(Date(item.fromTimeMillis))
                val toTime = dateFormat.format(Date(item.toTimeMillis))
                binding.alertTime.text = "$fromTime to $toTime"
                binding.stopButton.setOnClickListener { onStopClick(item.id) }
                binding.deleteButton.setOnClickListener { onDeleteClick(item.id) }
            } catch (e: Exception) {
                Log.e("AlertAdapter", "Error binding item: ${e.message}", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        try {
            val item = getItem(position)
            holder.bind(item, onStopClick, onDeleteClick)
        } catch (e: Exception) {
            android.util.Log.e("AlertAdapter", "Error binding ViewHolder at position $position: ${e.message}", e)
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
        override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
            return oldItem == newItem
        }
    }
}