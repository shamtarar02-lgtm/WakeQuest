package com.example.clockapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private var alarms: List<Alarm>,
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvAlarmTime)
        val tvLabel: TextView = view.findViewById(R.id.tvAlarmLabel)
        val swEnabled: SwitchCompat = view.findViewById(R.id.swAlarmEnabled)
        val tvRepeatDays: TextView = view.findViewById(R.id.tvRepeatDays)
        val tvRingtone: TextView = view.findViewById(R.id.tvRingtone)
        val tvSnoozeStatus: TextView = view.findViewById(R.id.tvSnoozeStatus)
        val ivAlarmIcon: ImageView = view.findViewById(R.id.ivAlarmIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.tvTime.text = alarm.timeText
        holder.tvLabel.text = alarm.label
        holder.tvRepeatDays.text = alarm.getRepeatDaysText()
        holder.tvRingtone.text = alarm.ringtoneName
        holder.tvSnoozeStatus.text = if (alarm.isSnoozeEnabled) "Snooze: ON" else "Snooze: OFF"
        
        // Update visual state based on enabled/disabled
        val alpha = if (alarm.isEnabled) 1.0f else 0.5f
        holder.tvTime.alpha = alpha
        holder.ivAlarmIcon.alpha = alpha
        
        holder.swEnabled.setOnCheckedChangeListener(null)
        holder.swEnabled.isChecked = alarm.isEnabled

        holder.swEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(alarm, isChecked)
            // Immediately update alpha for better feedback
            val newAlpha = if (isChecked) 1.0f else 0.5f
            holder.tvTime.alpha = newAlpha
            holder.ivAlarmIcon.alpha = newAlpha
        }
        
        holder.itemView.setOnLongClickListener {
            onDelete(alarm)
            true
        }
    }

    override fun getItemCount() = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }
}
