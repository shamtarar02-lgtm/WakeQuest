package com.example.clockapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MathMissionActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnSubmit: Button
    private var correctAnswer: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_mission)

        tvQuestion = findViewById(R.id.tvQuestion)
        etAnswer = findViewById(R.id.etAnswer)
        btnSubmit = findViewById(R.id.btnSubmit)

        generateQuestion()

        btnSubmit.setOnClickListener {
            val userAnswer = etAnswer.text.toString().toIntOrNull()
            if (userAnswer == correctAnswer) {
                stopAlarmService()
                finish()
            } else {
                Toast.makeText(this, "Wrong answer! Try again.", Toast.LENGTH_SHORT).show()
                generateQuestion()
                etAnswer.text.clear()
            }
        }
    }

    private fun generateQuestion() {
        val num1 = Random.nextInt(10, 50)
        val num2 = Random.nextInt(10, 50)
        correctAnswer = num1 + num2
        tvQuestion.text = "$num1 + $num2 = ?"
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Alarm Stopped. Good Morning!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        // Disable back button so user must solve the math problem
        Toast.makeText(this, "Solve the problem to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}