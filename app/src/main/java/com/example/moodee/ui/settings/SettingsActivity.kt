package com.example.moodee.ui.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.moodee.R
import com.example.moodee.data.PreferencesManager
import com.example.moodee.receivers.HydrationReceiver
import com.example.moodee.ui.home.HomeActivity
import com.example.moodee.ui.habits.HabitsActivity
import com.example.moodee.ui.mood.MoodActivity
import com.example.moodee.ui.onboarding.OnboardingActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager(this)

        val spinner = findViewById<Spinner>(R.id.spinnerInterval)
        val btnSave = findViewById<Button>(R.id.btnSaveInterval)
        val btnShare = findViewById<Button>(R.id.btnShareMood)
        val btnReset = findViewById<Button>(R.id.btnResetData)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Hydration options
        val intervals = arrayOf("Every 1 hour", "Every 2 hours", "Every 3 hours")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervals)

        btnSave.setOnClickListener {
            val selected = spinner.selectedItem.toString()
            prefs.saveString("hydration_interval", selected)
            scheduleHydrationReminder(selected)
            Toast.makeText(this, "Hydration reminder set: $selected", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            // Load from the same SharedPreferences where MoodActivity saves moods
            val sp = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
            val json = sp.getString("mood_history", "[]") ?: "[]"
            val arr = org.json.JSONArray(json)

            if (arr.length() == 0) {
                Toast.makeText(this, "No mood data to share", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sb = StringBuilder()
            sb.append("🌤️ My Mood Summary (last ${arr.length()} entries)\n\n")

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault())

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val date = obj.optLong("date", 0L)
                val emoji = obj.optString("emoji", "😊")
                val note = obj.optString("note", "")
                sb.append("${dateFormat.format(java.util.Date(date))}  $emoji  $note\n")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "My Mood Summary 😊")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }

            startActivity(Intent.createChooser(shareIntent, "Share Mood Summary"))
        }


        btnReset.setOnClickListener {
            prefs.clearAll()
            Toast.makeText(this, "All data reset.", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finishAffinity()
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navHabits = findViewById<LinearLayout>(R.id.navHabits)
        val navMood = findViewById<LinearLayout>(R.id.navMood)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings) // ✅ Add this

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navHabits.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
            finish()
        }

        navMood.setOnClickListener {
            startActivity(Intent(this, MoodActivity::class.java))
            finish()
        }
    }

    private fun scheduleHydrationReminder(selected: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HydrationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        val intervalHours = when {
            selected.contains("1") -> 1
            selected.contains("2") -> 2
            else -> 3
        }

        val trigger = System.currentTimeMillis() + intervalHours * 60 * 60 * 1000

        am.setRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            intervalHours * 60 * 60 * 1000L,
            pending
        )

        prefs.saveString("next_hydration_time", Date(trigger).toString())
    }
}
