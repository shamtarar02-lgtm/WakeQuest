package com.example.clockapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class StepMissionActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var stepCount = 0
    private val requiredSteps = 10
    private var lastMagnitude = 0f
    private var isStepDetected = false

    private lateinit var tvStepCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_mission)

        tvStepCount = findViewById(R.id.tvStepCount)
        updateUI()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
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

        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = magnitude - lastMagnitude
        lastMagnitude = magnitude

        // Basic step detection logic using accelerometer magnitude peaks
        if (magnitude > 12 && !isStepDetected) {
            isStepDetected = true
            stepCount++
            updateUI()

            if (stepCount >= requiredSteps) {
                stopAlarmService()
                finish()
            }
        } else if (magnitude < 9) {
            isStepDetected = false
        }
    }

    private fun updateUI() {
        tvStepCount.text = "$stepCount / $requiredSteps"
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Great job! You're out of bed.", Toast.LENGTH_LONG).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBackPressed() {
        Toast.makeText(this, "Walk 10 steps to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}