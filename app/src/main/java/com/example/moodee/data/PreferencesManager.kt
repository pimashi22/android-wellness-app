package com.example.moodee.data

import android.content.Context
import com.example.moodee.model.Habit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("MoodeePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveHabits(habits: List<Habit>) {
        val json = gson.toJson(habits)
        prefs.edit().putString("habits", json).apply()
    }

    fun loadHabits(): MutableList<Habit> {
        val json = prefs.getString("habits", null)
        if (json.isNullOrEmpty()) return mutableListOf()
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        return gson.fromJson(json, type)
    }

    // optional: today's mood (used by HomeActivity later)
    fun saveTodayMood(emoji: String) {
        prefs.edit().putString("today_mood", emoji).apply()
    }
    fun getTodayMood(): String? = prefs.getString("todaysMood", "😊")


    fun saveString(key: String, value: String) {
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    data class MoodEntry(
        val date: String,
        val emoji: String,
        val note: String
    )

    fun loadMoodHistory(): List<MoodEntry> {
        val json = prefs.getString("mood_history", "[]")
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<MoodEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMoodHistory(list: List<MoodEntry>) {
        val gson = com.google.gson.Gson()
        val json = gson.toJson(list)
        prefs.edit().putString("mood_history", json).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun loadString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }




}
