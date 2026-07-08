package com.example.clockapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var swDarkMode: MaterialSwitch
    private lateinit var swVibration: MaterialSwitch
    private lateinit var btnDefaultRingtone: MaterialButton
    private lateinit var actvChallengeType: AutoCompleteTextView
    private lateinit var sliderSnooze: Slider
    private lateinit var tvSnoozeValue: TextView

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(this, uri)
                val name = ringtone.getTitle(this)
                saveRingtone(uri.toString(), name)
                btnDefaultRingtone.text = "Default Ringtone: $name"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("ClockAppPrefs", Context.MODE_PRIVATE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        swDarkMode = findViewById(R.id.swDarkMode)
        swVibration = findViewById(R.id.swVibration)
        btnDefaultRingtone = findViewById(R.id.btnDefaultRingtone)
        actvChallengeType = findViewById(R.id.actvChallengeType)
        sliderSnooze = findViewById(R.id.sliderSnooze)
        tvSnoozeValue = findViewById(R.id.tvSnoozeValue)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", true)
        swDarkMode.isChecked = isDarkMode

        val isVibration = sharedPreferences.getBoolean("vibration", true)
        swVibration.isChecked = isVibration

        val ringtoneName = sharedPreferences.getString("ringtone_name", "Oxygen")
        btnDefaultRingtone.text = "Default Ringtone: $ringtoneName"

        val snoozeDuration = sharedPreferences.getInt("snooze_duration", 5)
        sliderSnooze.value = snoozeDuration.toFloat()
        tvSnoozeValue.text = "$snoozeDuration minutes"

        val challenges = resources.getStringArray(R.array.mission_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, challenges)
        actvChallengeType.setAdapter(adapter)
        
        val defaultChallenge = sharedPreferences.getString("default_mission", "Math Problem")
        actvChallengeType.setText(defaultChallenge, false)
    }

    private fun setupListeners() {
        swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        swVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("vibration", isChecked).apply()
        }

        btnDefaultRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Default Tone")
                val currentRingtone = sharedPreferences.getString("ringtone_uri", null)
                if (currentRingtone != null) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtone))
                }
            }
            ringtonePickerLauncher.launch(intent)
        }

        actvChallengeType.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            sharedPreferences.edit().putString("default_mission", selected).apply()
        }

        sliderSnooze.addOnChangeListener { _, value, _ ->
            val duration = value.toInt()
            tvSnoozeValue.text = "$duration minutes"
            sharedPreferences.edit().putInt("snooze_duration", duration).apply()
        }
    }

    private fun saveRingtone(uri: String, name: String) {
        sharedPreferences.edit()
            .putString("ringtone_uri", uri)
            .putString("ringtone_name", name)
            .apply()
    }
}
