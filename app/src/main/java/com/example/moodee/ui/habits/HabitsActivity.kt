package com.example.moodee.ui.habits

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.net.Uri
import android.widget.LinearLayout
import android.view.View
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodee.R
import com.example.moodee.ui.mood.MoodActivity
import com.example.moodee.ui.settings.SettingsActivity
import com.example.moodee.data.PreferencesManager
import com.example.moodee.model.Habit
import com.example.moodee.receivers.AlarmReceiver
import java.util.*
import android.content.pm.PackageManager


class HabitsActivity : AppCompatActivity() {

    private val TAG = "HabitsActivity"

    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: HabitsAdapter
    private lateinit var list: MutableList<Habit>
    private lateinit var recycler: RecyclerView
    private lateinit var btnAdd: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habits)

        // 🔔 Ask for notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        prefs = PreferencesManager(this)
        list = prefs.loadHabits()

        recycler = findViewById(R.id.recyclerHabits)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = HabitsAdapter(list,
            onToggle = { pos ->
                Log.d(TAG, "Toggled habit at $pos -> ${list[pos].doneToday}")
                prefs.saveHabits(list)
            },
            onEdit = { pos -> showHabitDialog(pos) },
            onDelete = { pos ->
                // ✅ Confirmation popup before delete
                AlertDialog.Builder(this)
                    .setTitle("Delete Habit")
                    .setMessage("Are you sure you want to delete this habit?")
                    .setPositiveButton("Yes") { _, _ ->
                        cancelHabitAlarm(list[pos])
                        list.removeAt(pos)
                        adapter.notifyItemRemoved(pos)
                        prefs.saveHabits(list)
                        Log.d(TAG, "Deleted habit at $pos. remaining=${list.size}")
                    }
                    .setNegativeButton("No", null)
                    .show()
            })

        recycler.adapter = adapter

        btnAdd = findViewById(R.id.btnAddHabit)
        btnAdd.setOnClickListener {
            showHabitDialog(-1)
        }



        // ✅ Bottom Navigation handling
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navHabits = findViewById<LinearLayout>(R.id.navHabits)
        val navMood = findViewById<LinearLayout>(R.id.navMood)


        navHome.setOnClickListener {
            startActivity(Intent(this, com.example.moodee.ui.home.HomeActivity::class.java))
            finish()
        }
        navHabits.setOnClickListener {
            // already here, maybe refresh or do nothing
        }

        navMood.setOnClickListener {
            startActivity(Intent(this, MoodActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }



        Log.d(TAG, "Loaded habits: ${list.size}")


    }


    private fun showHabitDialog(editPos: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_habit_form, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etHabitName)
        val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerFreq)
        val tp = dialogView.findViewById<TimePicker>(R.id.timePicker)
        tp.setIs24HourView(false)

        val freqAdapter = ArrayAdapter.createFromResource(this, R.array.freq_array, android.R.layout.simple_spinner_item)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = freqAdapter

        val isEdit = editPos >= 0
        if (isEdit) {
            val h = list[editPos]
            etName.setText(h.title)
            val pos = when (h.frequency) {
                "Daily" -> 0
                "Weekly" -> 1
                "Weekdays" -> 2
                "Weekends" -> 3
                else -> 0
            }
            spinner.setSelection(pos)
            if (Build.VERSION.SDK_INT >= 23) {
                tp.hour = h.timeHour
                tp.minute = h.timeMinute
            } else {
                @Suppress("DEPRECATION")
                tp.currentHour = h.timeHour
                @Suppress("DEPRECATION")
                tp.currentMinute = h.timeMinute
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                tp.hour = 9
                tp.minute = 0
            } else {
                @Suppress("DEPRECATION")
                tp.currentHour = 9
                @Suppress("DEPRECATION")
                tp.currentMinute = 0
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Habit" else "New Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etName.text.toString().trim()
                if (title.isEmpty()) {
                    // Provide feedback instead of silent return
                    android.widget.Toast.makeText(this, "Please enter a habit name", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val freq = spinner.selectedItem.toString()
                val hour = if (Build.VERSION.SDK_INT >= 23) tp.hour else tp.currentHour
                val minute = if (Build.VERSION.SDK_INT >= 23) tp.minute else tp.currentMinute

                if (isEdit) {
                    val h = list[editPos]
                    h.title = title; h.frequency = freq; h.timeHour = hour; h.timeMinute = minute
                    adapter.notifyItemChanged(editPos)
                    scheduleHabitAlarm(h)
                    Log.d(TAG, "Edited habit at $editPos -> $title")
                } else {
                    val h = Habit(title = title, frequency = freq, timeHour = hour, timeMinute = minute)
                    list.add(h)
                    adapter.notifyItemInserted(list.size - 1)
                    // scroll to show new item
                    recycler.post { recycler.smoothScrollToPosition(list.size - 1) }
                    scheduleHabitAlarm(h)
                    Log.d(TAG, "Added new habit -> $title (size=${list.size})")
                }
                prefs.saveHabits(list)
                Log.d(TAG, "Habits saved. total=${list.size}")
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun scheduleHabitAlarm(habit: Habit) {
        Log.d(TAG, "scheduleHabitAlarm() called for ${habit.title} at ${habit.timeHour}:${habit.timeMinute}")

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", habit.title)
            putExtra("text", "Reminder: ${habit.title}")
            putExtra("habit_id", habit.id)
            putExtra("frequency", habit.frequency)
            putExtra("hour", habit.timeHour)
            putExtra("minute", habit.timeMinute)
        }

        // request code must be stable and fit in int
        val requestCode = (habit.id % Int.MAX_VALUE).toInt()

        // PendingIntent flags (API 31+ requires explicit mutability)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val pending = PendingIntent.getBroadcast(this, requestCode, intent, flags)

        // build the calendar for the next occurrence
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, habit.timeHour)
            set(Calendar.MINUTE, habit.timeMinute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d(TAG, "Scheduling alarm for ${habit.title} at ${calendar.time} (freq=${habit.frequency}) req=$requestCode")

        // On Android 12+ check if app can schedule exact alarms; otherwise ask the user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!am.canScheduleExactAlarms()) {
                    // Ask user to allow exact alarms in system settings
                    Log.w(TAG, "App cannot schedule exact alarms. Asking user to enable it.")
                    AlertDialog.Builder(this)
                        .setTitle("Allow exact alarms")
                        .setMessage("To ensure habit reminders fire exactly on time, please allow Moodee to schedule exact alarms in system settings.")
                        .setPositiveButton("Open settings") { _, _ ->
                            try {
                                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                            } catch (ex: Exception) {
                                // fallback to app settings page
                                val uri = Uri.parse("package:$packageName")
                                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                                startActivity(i)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    // continue — we'll still attempt to schedule below and catch SecurityException
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking canScheduleExactAlarms()", e)
            }
        }

        // 🔹 DEBUG MODE: always schedule alarm 30 seconds from now (ignore calendar)
        val triggerAt = System.currentTimeMillis() + 20 * 1000 // 20 sec later

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            Log.d(TAG, "Exact alarm scheduled to fire in 30 sec (test mode).")
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm - falling back to non-exact", se)
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }

    }


    private fun cancelHabitAlarm(habit: Habit) {
        try {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            val flags = (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0) or PendingIntent.FLAG_NO_CREATE
            val pending = PendingIntent.getBroadcast(this, (habit.id % Int.MAX_VALUE).toInt(), intent, flags)
            pending?.let {
                am.cancel(it)
                Log.d(TAG, "Cancelled alarm for ${habit.title}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "cancel alarm error", e)
        }
    }
}
