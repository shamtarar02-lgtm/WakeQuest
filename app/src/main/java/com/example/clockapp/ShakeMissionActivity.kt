package com.example.clockapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class ShakeMissionActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeCount = 0
    private val requiredShakes = 30
    private var lastShakeTime: Long = 0

    private lateinit var pbShakeProgress: ProgressBar
    private lateinit var tvShakeCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_mission)

        pbShakeProgress = findViewById(R.id.pbShakeProgress)
        tvShakeCount = findViewById(R.id.tvShakeCount)

        pbShakeProgress.max = requiredShakes
        updateUI()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (acceleration > 12) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastShakeTime > 500) {
                lastShakeTime = currentTime
                shakeCount++
                updateUI()

                if (shakeCount >= requiredShakes) {
                    stopAlarmService()
                    finish()
                }
            }
        }
    }

    private fun updateUI() {
        pbShakeProgress.progress = shakeCount
        tvShakeCount.text = "$shakeCount / $requiredShakes"
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished! Good Morning!", Toast.LENGTH_LONG).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBackPressed() {
        Toast.makeText(this, "Keep shaking to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}