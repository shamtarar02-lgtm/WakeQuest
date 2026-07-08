package com.example.clockapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SortMissionActivity : AppCompatActivity() {

    private lateinit var glNumbers: GridLayout
    private var numbers = mutableListOf<Int>()
    private var sortedNumbers = mutableListOf<Int>()
    private var nextIndexToClick = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sort_mission)

        glNumbers = findViewById(R.id.glNumbers)
        generateNumbers()
    }

    private fun generateNumbers() {
        numbers = (1..100).shuffled().take(6).toMutableList()
        sortedNumbers = numbers.sorted().toMutableList()
        
        glNumbers.removeAllViews()
        for (num in numbers) {
            val button = Button(this).apply {
                text = num.toString()
                textSize = 24f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#333333"))
                setOnClickListener { onNumberClicked(this, num) }
                
                val params = GridLayout.LayoutParams().apply {
                    width = 250
                    height = 250
                    setMargins(10, 10, 10, 10)
                }
                layoutParams = params
            }
            glNumbers.addView(button)
        }
    }

    private fun onNumberClicked(button: Button, value: Int) {
        if (value == sortedNumbers[nextIndexToClick]) {
            button.visibility = View.INVISIBLE
            nextIndexToClick++
            
            if (nextIndexToClick == sortedNumbers.size) {
                stopAlarmService()
                finish()
            }
        } else {
            Toast.makeText(this, "Wrong! Starting over.", Toast.LENGTH_SHORT).show()
            nextIndexToClick = 0
            generateNumbers()
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        Toast.makeText(this, "Mission Complete!", Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Sort the numbers to stop the alarm!", Toast.LENGTH_SHORT).show()
    }
}