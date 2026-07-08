package com.example.clockapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var rvAlarms: RecyclerView
    private lateinit var btnSetAlarm: FloatingActionButton
    private lateinit var btnPickRingtone: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var emptyState: LinearLayout
    private lateinit var btnEmptyAdd: MaterialButton
    private lateinit var alarmAdapter: AlarmAdapter
    private var alarmList = mutableListOf<Alarm>()
    private lateinit var sharedPreferences: SharedPreferences

    private var pendingAlarmTime: Pair<Int, Int>? = null
    private var pendingLabel: String = ""
    private var pendingRepeatDays: List<Int> = emptyList()
    private var pendingSnooze: Boolean = true

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(this, uri)
                val name = ringtone.getTitle(this)
                saveRingtoneUri(uri.toString(), name)
                Toast.makeText(this, "Default Ringtone: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val qrSetupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCANNED_BARCODE")
            if (barcode != null && pendingAlarmTime != null) {
                createNewAlarm(pendingAlarmTime!!.first, pendingAlarmTime!!.second, pendingLabel, "QR", barcode, pendingRepeatDays, pendingSnooze)
            }
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        if (!cameraGranted && pendingAlarmTime != null) {
            Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("ClockAppPrefs", Context.MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        loadAlarms()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvAlarms = findViewById(R.id.rvAlarms)
        btnSetAlarm = findViewById(R.id.btnSetAlarm)
        btnPickRingtone = findViewById(R.id.btnPickRingtone)
        btnSettings = findViewById(R.id.btnSettings)
        emptyState = findViewById(R.id.emptyState)
        btnEmptyAdd = findViewById(R.id.btnEmptyAdd)

        setupRecyclerView()
        updateEmptyState()

        btnSetAlarm.setOnClickListener { checkPermissionsAndShowTimePicker() }
        btnEmptyAdd.setOnClickListener { checkPermissionsAndShowTimePicker() }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        btnPickRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                val currentRingtone = sharedPreferences.getString("ringtone_uri", null)
                if (currentRingtone != null) putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtone))
            }
            ringtonePickerLauncher.launch(intent)
        }
        checkInitialPermissions()
    }

    private fun applyTheme() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", true)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(alarmList, { alarm, isEnabled ->
            alarm.isEnabled = isEnabled
            if (isEnabled) scheduleAlarm(alarm) else cancelAlarm(alarm)
            saveAlarms()
        }, { alarm ->
            cancelAlarm(alarm)
            alarmList.remove(alarm)
            alarmAdapter.updateAlarms(alarmList)
            updateEmptyState()
            saveAlarms()
        })
        rvAlarms.layoutManager = LinearLayoutManager(this)
        rvAlarms.adapter = alarmAdapter
    }

    private fun updateEmptyState() {
        emptyState.visibility = if (alarmList.isEmpty()) View.VISIBLE else View.GONE
        rvAlarms.visibility = if (alarmList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun checkInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    private fun checkPermissionsAndShowTimePicker() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }
        showTimePicker()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select Alarm Time")
            .build()

        picker.addOnPositiveButtonClickListener { showAlarmDetailsDialog(picker.hour, picker.minute) }
        picker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun showAlarmDetailsDialog(hour: Int, minute: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_alarm_details, null)
        val etLabel = view.findViewById<EditText>(R.id.etAlarmLabel)
        val rgMission = view.findViewById<RadioGroup>(R.id.rgMissionType)
        val swSnooze = view.findViewById<MaterialSwitch>(R.id.swSnooze)
        val cgDays = view.findViewById<ChipGroup>(R.id.cgDays)

        val defaultMission = sharedPreferences.getString("default_mission", "Math Problem")
        val missionMap = mapOf("Shake Phone" to R.id.rbShake, "Typing Sentence" to R.id.rbTyping, "Color Match (Stroop)" to R.id.rbStroop, "Emoji Match" to R.id.rbEmoji, "Memory Cards" to R.id.rbMemory, "Sort Numbers" to R.id.rbSort, "Simon Says" to R.id.rbSimon, "Step Mission" to R.id.rbStep, "Move Mission" to R.id.rbMove, "Light Mission" to R.id.rbLight, "QR Scan" to R.id.rbQR)
        rgMission.check(missionMap[defaultMission] ?: R.id.rbMath)

        MaterialAlertDialogBuilder(this)
            .setTitle("Alarm Details")
            .setView(view)
            .setPositiveButton("Set") { _, _ ->
                val label = etLabel.text.toString().ifEmpty { "Alarm" }
                val repeatDays = mutableListOf<Int>()
                val dayChips = listOf(R.id.chipMon, R.id.chipTue, R.id.chipWed, R.id.chipThu, R.id.chipFri, R.id.chipSat, R.id.chipSun)
                dayChips.forEachIndexed { i, id -> if (view.findViewById<Chip>(id).isChecked) repeatDays.add(i + 1) }

                val missionType = when (rgMission.checkedRadioButtonId) {
                    R.id.rbShake -> "Shake"; R.id.rbTyping -> "Typing"; R.id.rbStroop -> "Stroop"
                    R.id.rbEmoji -> "Emoji"; R.id.rbMemory -> "Memory"; R.id.rbSort -> "Sort"
                    R.id.rbSimon -> "Simon"; R.id.rbStep -> "Step"; R.id.rbMove -> "Move"
                    R.id.rbLight -> "Light"; R.id.rbQR -> "QR"; else -> "Math"
                }
                
                if (missionType == "QR" && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    pendingAlarmTime = Pair(hour, minute); pendingLabel = label; pendingRepeatDays = repeatDays; pendingSnooze = swSnooze.isChecked
                    permissionsLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else if (missionType == "QR") {
                    pendingAlarmTime = Pair(hour, minute); pendingLabel = label; pendingRepeatDays = repeatDays; pendingSnooze = swSnooze.isChecked
                    startQrSetup()
                } else {
                    createNewAlarm(hour, minute, label, missionType, null, repeatDays, swSnooze.isChecked)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startQrSetup() {
        qrSetupLauncher.launch(Intent(this, QrScannerActivity::class.java).apply { putExtra("IS_SETTING_MODE", true) })
    }

    private fun createNewAlarm(hour: Int, minute: Int, label: String, missionType: String, targetBarcode: String?, repeatDays: List<Int>, snooze: Boolean) {
        val rUri = sharedPreferences.getString("ringtone_uri", null)
        val rName = sharedPreferences.getString("ringtone_name", "Default")
        val newAlarm = Alarm(System.currentTimeMillis().toInt(), hour, minute, label, true, missionType, targetBarcode, repeatDays, rName, rUri, snooze)
        alarmList.add(newAlarm)
        alarmAdapter.updateAlarms(alarmList)
        updateEmptyState()
        scheduleAlarm(newAlarm)
        saveAlarms()
    }

    private fun scheduleAlarm(alarm: Alarm) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("MISSION_TYPE", alarm.missionType)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("IS_SNOOZE_ENABLED", alarm.isSnoozeEnabled)
            putExtra("TARGET_BARCODE", alarm.targetBarcode)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour); set(Calendar.MINUTE, alarm.minute); set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun saveAlarms() { sharedPreferences.edit().putString("alarms", Gson().toJson(alarmList)).apply() }

    private fun loadAlarms() {
        val json = sharedPreferences.getString("alarms", null)
        if (json != null) {
            val type = object : TypeToken<List<Alarm>>() {}.type
            alarmList = Gson().fromJson(json, type)
        }
    }

    private fun saveRingtoneUri(uri: String, name: String) {
        sharedPreferences.edit().putString("ringtone_uri", uri).putString("ringtone_name", name).apply()
    }
}
