package com.example.clockapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmojiMissionActivity : AppCompatActivity() {

    private lateinit var tvTargetEmoji: TextView
    private lateinit var glEmojiGrid: GridLayout
    private lateinit var tvEmojiProgress: TextView

    private val emojis = listOf("😀", "😂", "🚀", "💎", "🔥", "🌈", "🍕", "🎸", "🐱", "🦄", "⚽", "🍔")
    private lateinit var targetEmoji: String
    private var score = 0
    private val requiredScore = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emoji_mission)

        tvTargetEmoji = findViewById(R.id.tvTargetEmoji)
        glEmojiGrid = findViewById(R.id.glEmojiGrid)
        tvEmojiProgress = findViewById(R.id.tvEmojiProgress)

        generateChallenge()
    }

    private fun generateChallenge() {
        targetEmoji = emojis.random()
        tvTargetEmoji.text = targetEmoji
        tvEmojiProgress.text = "Score: $score / $requiredScore"

        glEmojiGrid.removeAllViews()
        val gridEmojis = emojis.shuffled().take(9).toMutableList()
        if (!gridEmojis.contains(targetEmoji)) {
            gridEmojis[0] = targetEmoji
            gridEmojis.shuffle()
        }

        for (emoji in gridEmojis) {
            val button = Button(this).apply {
                text = emoji
                textSize = 32f
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    if (emoji == targetEmoji) {
                        score++
                        if (score >= requiredScore) {
                            stopAlarmService()
                            finish()
                        } else {
                            generateChallenge()
                        }
                    } else {
                        Toast.makeText(this@EmojiMissionActivity, "Wrong Emoji! Score reset.", Toast.LENGTH_SHORT).show()
                        score = 0
                        generateChallenge()
                    }
                }
                
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 250
                    height = 250
                    setMargins(10, 10, 10, 10)
                }
            }
            glEmojiGrid.addView(button)
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Accomplished! Good Morning!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Find the matching emoji to stop!", Toast.LENGTH_SHORT).show()
    }
}