package com.example.clockapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class StroopMissionActivity : AppCompatActivity() {

    private lateinit var tvStroopWord: TextView
    private lateinit var glColorButtons: GridLayout
    private lateinit var tvStroopProgress: TextView

    private val colors = listOf(
        "RED" to "#FF0000",
        "BLUE" to "#0000FF",
        "GREEN" to "#00FF00",
        "YELLOW" to "#FFFF00",
        "PURPLE" to "#800080",
        "ORANGE" to "#FFA500"
    )

    private var targetColorHex: String = ""
    private var score = 0
    private val requiredScore = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_mission)

        tvStroopWord = findViewById(R.id.tvStroopWord)
        glColorButtons = findViewById(R.id.glColorButtons)
        tvStroopProgress = findViewById(R.id.tvStroopProgress)

        generateChallenge()
    }

    private fun generateChallenge() {
        val wordIndex = Random.nextInt(colors.size)
        val colorIndex = Random.nextInt(colors.size)

        val wordText = colors[wordIndex].first
        targetColorHex = colors[colorIndex].second

        tvStroopWord.text = wordText
        tvStroopWord.setTextColor(Color.parseColor(targetColorHex))

        tvStroopProgress.text = "Score: $score / $requiredScore"

        setupButtons()
    }

    private fun setupButtons() {
        glColorButtons.removeAllViews()
        val shuffledColors = colors.shuffled().take(4)
        
        // Ensure target color is in the list
        val buttonColors = if (shuffledColors.any { it.second == targetColorHex }) {
            shuffledColors
        } else {
            shuffledColors.take(3) + colors.first { it.second == targetColorHex }
        }.shuffled()

        for (colorPair in buttonColors) {
            val button = Button(this).apply {
                text = colorPair.first
                setBackgroundColor(Color.parseColor(colorPair.second))
                setTextColor(if (colorPair.first == "YELLOW") Color.BLACK else Color.WHITE)
                setOnClickListener {
                    if (colorPair.second == targetColorHex) {
                        score++
                        if (score >= requiredScore) {
                            stopAlarmService()
                            finish()
                        } else {
                            generateChallenge()
                        }
                    } else {
                        Toast.makeText(this@StroopMissionActivity, "Wrong! Resetting score.", Toast.LENGTH_SHORT).show()
                        score = 0
                        generateChallenge()
                    }
                }
                
                val params = GridLayout.LayoutParams().apply {
                    width = 300
                    height = 200
                    setMargins(10, 10, 10, 10)
                }
                layoutParams = params
            }
            glColorButtons.addView(button)
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished! Good Morning!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Complete the mission to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}