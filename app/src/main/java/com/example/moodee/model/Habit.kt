package com.example.moodee.model

data class Habit(
    var id: Long = System.currentTimeMillis(),
    var title: String = "",
    var frequency: String = "Daily",
    var timeHour: Int = 9,
    var timeMinute: Int = 0,
    var doneToday: Boolean = false
)
