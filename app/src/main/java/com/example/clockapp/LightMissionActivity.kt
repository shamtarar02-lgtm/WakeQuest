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

class LightMissionActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var pbLight: ProgressBar
    private lateinit var tvLightValue: TextView
    private val targetLux = 100f // Required light level to stop alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light_mission)

        pbLight = findViewById(R.id.pbLightProgress)
        tvLightValue = findViewById(R.id.tvLightValue)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            Toast.makeText(this, "Light sensor not found! Skipping mission.", Toast.LENGTH_SHORT).show()
            stopAlarmService()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val lux = event.values[0]
        
        tvLightValue.text = "Current Light: ${lux.toInt()} Lux"
        pbLight.progress = lux.toInt().coerceAtMost(100)

        if (lux >= targetLux) {
            stopAlarmService()
            finish()
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Room is bright! Good Morning!", Toast.LENGTH_LONG).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    override fun onBackPressed() {
        Toast.makeText(this, "Turn on the lights to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}