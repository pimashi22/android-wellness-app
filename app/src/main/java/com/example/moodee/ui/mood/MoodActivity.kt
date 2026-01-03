package com.example.moodee.ui.mood

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moodee.R
import com.example.moodee.ui.habits.HabitsActivity
import com.example.moodee.ui.home.HomeActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import com.example.moodee.data.PreferencesManager
import com.example.moodee.ui.settings.SettingsActivity
import java.util.*

class MoodActivity : AppCompatActivity() {

    private val KEY_MOOD_HISTORY = "mood_history"
    private val KEY_TODAY_MOOD = "todaysMood"

    private lateinit var tvTodaysMood: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var moodListContainer: LinearLayout
    private lateinit var moodChart: LineChart
    private lateinit var btnAddMood: Button

    private val moodHistory = mutableListOf<MoodEntry>()

    // currently selected date in the calendar (normalized to midnight millis)
    private var selectedDayMillis: Long = 0L

    private lateinit var prefs: PreferencesManager


    override fun onCreate(savedInstanceState: Bundle?) {

        prefs = PreferencesManager(this)


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood)

        tvTodaysMood = findViewById(R.id.tvTodaysMood)
        calendarView = findViewById(R.id.calendarView)
        moodListContainer = findViewById(R.id.moodListContainer)
        moodChart = findViewById(R.id.moodChart)
        btnAddMood = findViewById(R.id.btnAddMood)

        // load persisted moods
        loadMoodsFromPrefs()

        // set initial selected day = today
        selectedDayMillis = normalizeToDayMillis(System.currentTimeMillis())
        calendarView.date = selectedDayMillis

        // show today's mood if saved
        val prefs = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
        val todayMood = prefs.getString(KEY_TODAY_MOOD, "😊")
        tvTodaysMood.text = todayMood

        // show mood entries for the selected date and draw chart for the week ending that date
        showMoodsForDate(selectedDayMillis)
        updateMoodChart(selectedDayMillis)

        // when user taps another date in calendar, update selectedDayMillis, list & chart
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            selectedDayMillis = normalizeToDayMillis(cal.timeInMillis)
            showMoodsForDate(selectedDayMillis)
            updateMoodChart(selectedDayMillis)
        }

        // add mood: pass the currently selected day to the dialog
        btnAddMood.setOnClickListener {
            showAddMoodDialog(selectedDayMillis)
        }

        // bottom nav hookups (assumes same bottom_navigation IDs)
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navHabits).setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMood).setOnClickListener {
            // already here
        }

        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

    }

    // --- Dialog for adding a mood entry (date taken from selectedDayMillis)
    private fun showAddMoodDialog(selectedDay: Long) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_mood, null)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvTime)
        val spinnerEmoji = dialogView.findViewById<Spinner>(R.id.spinnerEmoji)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)

        // default time = now
        val calNow = Calendar.getInstance()
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        tvTime.text = timeFmt.format(calNow.time)

        // emoji list
        val emojis = arrayOf("🤩", "😊", "😐", "😢", "😡", "😴")
        spinnerEmoji.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emojis)

        // time picker
        tvTime.setOnClickListener {
            val t = Calendar.getInstance()
            val tp = TimePickerDialog(this, { _, hour, minute ->
                val tmp = Calendar.getInstance()
                tmp.set(Calendar.HOUR_OF_DAY, hour)
                tmp.set(Calendar.MINUTE, minute)
                tvTime.text = timeFmt.format(tmp.time)
            }, t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE), false)
            tp.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Mood")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    // build full datetime
                    val timeStr = tvTime.text.toString()
                    val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDay))
                    val dtFmt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                    val dt = dtFmt.parse("$datePart $timeStr") ?: Date()
                    val dtMillis = dt.time

                    val emoji = spinnerEmoji.selectedItem.toString()
                    val note = etNote.text.toString().trim()

                    // save new entry
                    val entry = MoodEntry(dtMillis, emoji, note)
                    moodHistory.add(entry)
                    saveMoodsToPrefs()

                    // ✅ store in PreferencesManager (for Settings page sharing)
                    val prefsManager = PreferencesManager(this)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDay))
                    val savedList = prefsManager.loadMoodHistory().toMutableList()
                    savedList.add(PreferencesManager.MoodEntry(dateStr, emoji, note))
                    prefsManager.saveMoodHistory(savedList)

                    // ✅ also update today's mood everywhere
                    val today = normalizeToDayMillis(System.currentTimeMillis())
                    if (normalizeToDayMillis(dtMillis) == today) {
                        prefs.saveTodayMood(emoji) // uses PreferencesManager method
                        val shared = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
                        shared.edit().putString("todaysMood", emoji).apply()
                        tvTodaysMood.text = emoji
                    }

                    // refresh
                    showMoodsForDate(selectedDay)
                    updateMoodChart(selectedDay)
                    Toast.makeText(this, "Mood saved", Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(this, "Could not save mood: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // --- Show moods for a given date (normalized day millis)
    private fun showMoodsForDate(dayMillis: Long) {
        moodListContainer.removeAllViews()

        val entries = moodHistory
            .filter { normalizeToDayMillis(it.dateMillis) == dayMillis }
            .sortedByDescending { it.dateMillis }

        if (entries.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No moods recorded for this date."
            tv.setTextColor(ContextCompat.getColor(this, R.color.textSecondary))
            moodListContainer.addView(tv)
            return
        }

        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        entries.forEach { e ->
            val item = LinearLayout(this)
            item.orientation = LinearLayout.HORIZONTAL
            item.setPadding(12, 10, 12, 10)

            val tvEmoji = TextView(this)
            tvEmoji.text = e.emoji
            tvEmoji.textSize = 26f
            tvEmoji.setPadding(0, 0, 12, 0)

            val tvInfo = TextView(this)
            val t = timeFmt.format(Date(e.dateMillis))
            tvInfo.text = "$t  •  ${e.note}"
            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.secondaryColor))

            item.addView(tvEmoji)
            item.addView(tvInfo)

            // long press to delete entry
            item.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete entry")
                    .setMessage("Delete this mood entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        moodHistory.remove(e)
                        saveMoodsToPrefs()
                        showMoodsForDate(dayMillis)
                        updateMoodChart(dayMillis)
                        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            moodListContainer.addView(item)
        }
    }

    // --- Chart: weekly (7-day) ending at 'endDayMillis'
    private fun updateMoodChart(endDayMillis: Long = normalizeToDayMillis(System.currentTimeMillis())) {
        val emojiScale = mapOf(
            "🤩" to 5f,
            "😊" to 4f,
            "😐" to 3f,
            "😴" to 2.5f,
            "😢" to 2f,
            "😡" to 1f
        )

        val entries = ArrayList<Entry>()
        val xLabels = ArrayList<String>()

        val cal = Calendar.getInstance()
        cal.timeInMillis = endDayMillis

        // Build 7 points (6 days before -> today)
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = cal.timeInMillis
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayMillis = normalizeToDayMillis(dayCal.timeInMillis)

            val dayEntries = moodHistory.filter { normalizeToDayMillis(it.dateMillis) == dayMillis }
            val avg = if (dayEntries.isEmpty()) 0f
            else dayEntries.map { emojiScale[it.emoji] ?: 3f }.average().toFloat()

            val xPos = (6 - i).toFloat()
            entries.add(Entry(xPos, avg))

            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(dayCal.time)
            xLabels.add(dayLabel)
        }

        val set = LineDataSet(entries, "Mood (0-5)")
        set.color = android.graphics.Color.parseColor("#8B0000") // dark red
        set.lineWidth = 3f
        set.setCircleColor(android.graphics.Color.parseColor("#8B0000"))
        set.circleRadius = 5f
        set.valueTextSize = 10f
        set.valueTextColor = ContextCompat.getColor(this, R.color.secondaryColor)

        val lineData = LineData(set)
        moodChart.data = lineData

        val xAxis = moodChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelCount = 7
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.secondaryColor)

        moodChart.axisRight.isEnabled = false
        moodChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.secondaryColor)
        moodChart.axisLeft.axisMinimum = 0f
        moodChart.axisLeft.axisMaximum = 5f

        moodChart.description.isEnabled = false
        moodChart.legend.isEnabled = false
        moodChart.animateX(600)
        moodChart.invalidate()
    }

    // --- persistence helpers
    private fun loadMoodsFromPrefs() {
        moodHistory.clear()
        val prefs = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
        val json = prefs.getString(KEY_MOOD_HISTORY, "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val dateMillis = o.optLong("date", System.currentTimeMillis())
                val emoji = o.optString("emoji", "😊")
                val note = o.optString("note", "")
                moodHistory.add(MoodEntry(dateMillis, emoji, note))
            }
        } catch (ex: Exception) {
            // ignore malformed
        }
    }

    private fun saveMoodsToPrefs() {
        val arr = JSONArray()
        moodHistory.forEach {
            val o = JSONObject()
            o.put("date", it.dateMillis)
            o.put("emoji", it.emoji)
            o.put("note", it.note)
            arr.put(o)
        }
        val prefs = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
        prefs.edit().putString(KEY_MOOD_HISTORY, arr.toString()).apply()
    }

    private fun normalizeToDayMillis(ts: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    data class MoodEntry(val dateMillis: Long, val emoji: String, val note: String)
}
