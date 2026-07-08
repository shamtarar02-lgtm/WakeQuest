package com.example.clockapp

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

class MoveMissionActivity : AppCompatActivity() {

    private lateinit var btnMoving: Button
    private lateinit var tvProgress: TextView
    private lateinit var rootLayout: ConstraintLayout
    private var tapCount = 0
    private val requiredTaps = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_move_mission)

        btnMoving = findViewById(R.id.btnMoving)
        tvProgress = findViewById(R.id.tvMoveProgress)
        rootLayout = findViewById(R.id.clMoveRoot)

        rootLayout.post {
            moveButton()
        }

        btnMoving.setOnClickListener {
            tapCount++
            tvProgress.text = "Taps: $tapCount / $requiredTaps"

            if (tapCount >= requiredTaps) {
                stopAlarmService()
                finish()
            } else {
                moveButton()
            }
        }
    }

    private fun moveButton() {
        val width = rootLayout.width
        val height = rootLayout.height

        val maxX = width - btnMoving.width
        val maxY = height - btnMoving.height

        // Ensure the button doesn't go behind the top text
        val minY = tvProgress.bottom + 50

        if (maxX > 0 && maxY > minY) {
            val randomX = Random.nextInt(0, maxX).toFloat()
            val randomY = Random.nextInt(minY, maxY).toFloat()

            btnMoving.x = randomX
            btnMoving.y = randomY
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished! You are awake now.", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Catch the button to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}