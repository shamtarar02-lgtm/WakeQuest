package com.example.clockapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TypingMissionActivity : AppCompatActivity() {

    private lateinit var tvTargetText: TextView
    private lateinit var etTypingInput: EditText
    private lateinit var btnSubmit: Button
    
    private val phrases = listOf(
        "The quick brown fox jumps over the lazy dog",
        "Early to bed and early to rise makes a man healthy",
        "Practice makes a man perfect",
        "Innovation distinguishes between a leader and a follower",
        "Stay hungry stay foolish"
    )
    private lateinit var targetPhrase: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_typing_mission)

        tvTargetText = findViewById(R.id.tvTargetText)
        etTypingInput = findViewById(R.id.etTypingInput)
        btnSubmit = findViewById(R.id.btnSubmitTyping)

        targetPhrase = phrases.random()
        tvTargetText.text = targetPhrase

        btnSubmit.setOnClickListener {
            val userInput = etTypingInput.text.toString().trim()
            if (userInput.equals(targetPhrase, ignoreCase = false)) {
                stopAlarmService()
                finish()
            } else {
                Toast.makeText(this, "Text doesn't match! Type carefully.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Good Morning! You are wide awake.", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Type the text to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}