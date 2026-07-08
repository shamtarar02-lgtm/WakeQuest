package com.example.clockapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val missionType = intent.getStringExtra("MISSION_TYPE") ?: "Math"
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val isSnoozeEnabled = intent.getBooleanExtra("IS_SNOOZE_ENABLED", true)
        val targetBarcode = intent.getStringExtra("TARGET_BARCODE")
        
        if (action == "SNOOZE_ALARM") {
            snoozeAlarm(context, label, missionType, ringtoneUri, isSnoozeEnabled, targetBarcode)
        } else {
            startAlarmService(context, label, missionType, ringtoneUri, isSnoozeEnabled, targetBarcode)
        }
    }

    private fun startAlarmService(context: Context, label: String, missionType: String, ringtoneUri: String?, isSnoozeEnabled: Boolean, targetBarcode: String?) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_LABEL", label)
            putExtra("MISSION_TYPE", missionType)
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("IS_SNOOZE_ENABLED", isSnoozeEnabled)
            putExtra("TARGET_BARCODE", targetBarcode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun snoozeAlarm(context: Context, label: String, missionType: String, ringtoneUri: String?, isSnoozeEnabled: Boolean, targetBarcode: String?) {
        val sharedPreferences = context.getSharedPreferences("ClockAppPrefs", Context.MODE_PRIVATE)
        val snoozeMinutes = sharedPreferences.getInt("snooze_duration", 5)

        val stopIntent = Intent(context, AlarmService::class.java)
        context.stopService(stopIntent)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_LABEL", label)
            putExtra("MISSION_TYPE", missionType)
            putExtra("RINGTONE_URI", ringtoneUri)
            putExtra("IS_SNOOZE_ENABLED", isSnoozeEnabled)
            putExtra("TARGET_BARCODE", targetBarcode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Toast.makeText(context, "Alarm ($label) snoozed for $snoozeMinutes minutes", Toast.LENGTH_SHORT).show()
    }
}
