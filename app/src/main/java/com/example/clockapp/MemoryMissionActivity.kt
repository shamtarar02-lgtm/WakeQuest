package com.example.clockapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MemoryMissionActivity : AppCompatActivity() {

    private lateinit var glMemoryGrid: GridLayout
    private val emojis = listOf("🍎", "🍌", "🍇", "🍉", "🍒", "🍓")
    private var cards = mutableListOf<String>()
    private var firstClickedButton: Button? = null
    private var firstClickedEmoji: String? = null
    private var pairsFound = 0
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_mission)

        glMemoryGrid = findViewById(R.id.glMemoryGrid)
        setupGame()
    }

    private fun setupGame() {
        cards = (emojis + emojis).shuffled().toMutableList()
        glMemoryGrid.removeAllViews()

        val buttonSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics
        ).toInt()
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
        ).toInt()

        for (emoji in cards) {
            val button = Button(this).apply {
                text = "?"
                textSize = 24f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#333333"))
                
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSize
                    height = buttonSize
                    setMargins(margin, margin, margin, margin)
                }
                
                setOnClickListener { onCardClicked(this, emoji) }
            }
            glMemoryGrid.addView(button)
        }
    }

    private fun onCardClicked(button: Button, emoji: String) {
        if (isProcessing || button == firstClickedButton || button.text != "?") return

        button.text = emoji
        button.setBackgroundColor(Color.parseColor("#555555"))

        if (firstClickedButton == null) {
            firstClickedButton = button
            firstClickedEmoji = emoji
        } else {
            if (emoji == firstClickedEmoji) {
                // Match found
                button.setBackgroundColor(Color.parseColor("#2E7D32")) // Green
                firstClickedButton?.setBackgroundColor(Color.parseColor("#2E7D32"))
                
                pairsFound++
                firstClickedButton = null
                firstClickedEmoji = null
                
                if (pairsFound == emojis.size) {
                    stopAlarmService()
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 500)
                }
            } else {
                // No match
                isProcessing = true
                button.setBackgroundColor(Color.parseColor("#C62828")) // Red
                Handler(Looper.getMainLooper()).postDelayed({
                    button.text = "?"
                    button.setBackgroundColor(Color.parseColor("#333333"))
                    firstClickedButton?.text = "?"
                    firstClickedButton?.setBackgroundColor(Color.parseColor("#333333"))
                    firstClickedButton = null
                    firstClickedEmoji = null
                    isProcessing = false
                }, 800)
            }
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Complete the mission to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}
