package com.example.clockapp

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    var label: String = "Alarm",
    var isEnabled: Boolean = true,
    var missionType: String = "Math",
    var targetBarcode: String? = null,
    var repeatDays: List<Int>? = emptyList(),
    var ringtoneName: String? = "Default",
    var ringtoneUri: String? = null,
    var isSnoozeEnabled: Boolean = true
) {
    val timeText: String
        get() = String.format("%02d:%02d", hour, minute)

    fun getRepeatDaysText(): String {
        val days = repeatDays ?: emptyList()
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return days.sorted().joinToString(", ") { dayNames.getOrNull(it - 1) ?: "" }
    }
}
