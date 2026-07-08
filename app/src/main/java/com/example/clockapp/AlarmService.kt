package com.example.clockapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isFlashOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var brightness = 0.1f
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_ALARM_DIRECT") {
            stopSelf()
            return START_NOT_STICKY
        }

        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val missionType = intent?.getStringExtra("MISSION_TYPE") ?: "Math"
        
        val sharedPreferences = getSharedPreferences("ClockAppPrefs", Context.MODE_PRIVATE)
        val snoozeMinutes = sharedPreferences.getInt("snooze_duration", 5)
        val isVibrationEnabled = sharedPreferences.getBoolean("vibration", true)

        startForegroundService(label, missionType, snoozeMinutes, isVibrationEnabled)
        playRingtone()
        startFlashlightBlink()
        startSunriseEffect()
        if (isVibrationEnabled) {
            startVibration()
        }

        return START_STICKY
    }

    private fun startForegroundService(label: String, missionType: String, snoozeMinutes: Int, vibration: Boolean) {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Alarm Ringing", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(vibration)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val missionIntent = when (missionType) {
            "Shake" -> Intent(this, ShakeMissionActivity::class.java)
            "Typing" -> Intent(this, TypingMissionActivity::class.java)
            "Sort" -> Intent(this, SortMissionActivity::class.java)
            "Stroop" -> Intent(this, StroopMissionActivity::class.java)
            "Emoji" -> Intent(this, EmojiMissionActivity::class.java)
            "Memory" -> Intent(this, MemoryMissionActivity::class.java)
            "Simon" -> Intent(this, SimonSaysMissionActivity::class.java)
            "Step" -> Intent(this, StepMissionActivity::class.java)
            "Move" -> Intent(this, MoveMissionActivity::class.java)
            "Light" -> Intent(this, LightMissionActivity::class.java)
            "QR" -> Intent(this, QrScannerActivity::class.java)
            else -> Intent(this, MathMissionActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_LABEL", label)
        }

        val missionPendingIntent = PendingIntent.getActivity(
            this, 0, missionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "SNOOZE_ALARM"
            putExtra("ALARM_LABEL", label)
            putExtra("MISSION_TYPE", missionType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm: $label")
            .setContentText("Wake up! Mission: $missionType")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(missionPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Solve & Stop", missionPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze (${snoozeMinutes}m)", snoozePendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun startFlashlightBlink() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.get(0)
        } catch (e: Exception) { e.printStackTrace() }

        handler.post(object : Runnable {
            override fun run() {
                try {
                    cameraId?.let { id ->
                        isFlashOn = !isFlashOn
                        cameraManager?.setTorchMode(id, isFlashOn)
                    }
                } catch (e: Exception) { e.printStackTrace() }
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun startSunriseEffect() {
        handler.post(object : Runnable {
            override fun run() {
                if (brightness < 1.0f) {
                    brightness += 0.05f
                    val intent = Intent("UPDATE_BRIGHTNESS")
                    intent.putExtra("level", brightness)
                    sendBroadcast(intent)
                    handler.postDelayed(this, 3000)
                }
            }
        })
    }

    private fun playRingtone() {
        if (mediaPlayer?.isPlaying == true) return
        try {
            val sharedPreferences = getSharedPreferences("ClockAppPrefs", Context.MODE_PRIVATE)
            val ringtoneUriString = sharedPreferences.getString("ringtone_uri", null)
            val alarmUri = ringtoneUriString?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            cameraId?.let { cameraManager?.setTorchMode(it, false) }
        } catch (e: Exception) { e.printStackTrace() }
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}