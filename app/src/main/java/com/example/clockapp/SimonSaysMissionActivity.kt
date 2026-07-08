package com.example.clockapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SimonSaysMissionActivity : AppCompatActivity() {

    private lateinit var vRed: View
    private lateinit var vBlue: View
    private lateinit var vGreen: View
    private lateinit var vYellow: View
    private lateinit var btnStart: Button

    private val sequence = mutableListOf<Int>()
    private var userSequenceIndex = 0
    private var isDisplayingSequence = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simon_says_mission)

        vRed = findViewById(R.id.vRed)
        vBlue = findViewById(R.id.vBlue)
        vGreen = findViewById(R.id.vGreen)
        vYellow = findViewById(R.id.vYellow)
        btnStart = findViewById(R.id.btnStartSimon)

        val views = listOf(vRed, vBlue, vGreen, vYellow)
        views.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (!isDisplayingSequence && sequence.isNotEmpty()) {
                    onViewClicked(index)
                }
            }
        }

        btnStart.setOnClickListener {
            startNewGame()
        }
    }

    private fun startNewGame() {
        sequence.clear()
        userSequenceIndex = 0
        btnStart.visibility = View.GONE
        addNewStep()
    }

    private fun addNewStep() {
        sequence.add((0..3).random())
        displaySequence()
    }

    private fun displaySequence() {
        isDisplayingSequence = true
        userSequenceIndex = 0
        val handler = Handler(Looper.getMainLooper())
        
        sequence.forEachIndexed { index, viewIndex ->
            handler.postDelayed({
                flashView(viewIndex)
                if (index == sequence.size - 1) {
                    handler.postDelayed({ isDisplayingSequence = false }, 600)
                }
            }, (index + 1) * 800L)
        }
    }

    private fun flashView(index: Int) {
        val view = when (index) {
            0 -> vRed
            1 -> vBlue
            2 -> vGreen
            else -> vYellow
        }
        val originalAlpha = 0.5f
        view.alpha = 1.0f
        Handler(Looper.getMainLooper()).postDelayed({
            view.alpha = originalAlpha
        }, 500)
    }

    private fun onViewClicked(index: Int) {
        flashView(index)
        if (index == sequence[userSequenceIndex]) {
            userSequenceIndex++
            if (userSequenceIndex == sequence.size) {
                if (sequence.size >= 4) { // Finish after 4 steps
                    stopAlarmService()
                    finish()
                } else {
                    Toast.makeText(this, "Correct! Next step...", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({ addNewStep() }, 1000)
                }
            }
        } else {
            Toast.makeText(this, "Wrong! Try again from start.", Toast.LENGTH_SHORT).show()
            startNewGame()
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Complete the sequence to stop!", Toast.LENGTH_SHORT).show()
    }
}