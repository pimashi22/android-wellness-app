package com.example.moodee.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.moodee.R
import com.example.moodee.ui.mood.MoodActivity
import com.example.moodee.ui.settings.SettingsActivity
import com.example.moodee.data.PreferencesManager
import com.example.moodee.ui.habits.HabitsActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var tvTotal: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var navHabits: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        prefs = PreferencesManager(this)

        // UI references
        tvTotal = findViewById(R.id.tvTotalHabits)
        tvCompleted = findViewById(R.id.tvCompletedHabits)
        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)

        val tvTodaysMood = findViewById<TextView>(R.id.tvTodaysMood)


        // Only Habits nav for now
        navHabits = findViewById(R.id.navHabits)
        navHabits.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }

        val navMood = findViewById<LinearLayout>(R.id.navMood)
        navMood.setOnClickListener {
            startActivity(Intent(this, MoodActivity::class.java))
            finish()
        }

        val navSettings = findViewById<LinearLayout>(R.id.navSettings)
        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }


    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun refreshDashboard() {

        // Load today's mood from SharedPreferences
        val sharedPrefs = getSharedPreferences("moodee_prefs", MODE_PRIVATE)
        val todayMood = sharedPrefs.getString("todaysMood", "😊")
        findViewById<TextView>(R.id.tvTodaysMood).text = todayMood


        // Get habits from prefs
        val habits = prefs.loadHabits()
        val total = habits.size
        val completed = habits.count { it.doneToday }

        tvTotal.text = "Total\n$total"
        tvCompleted.text = "Completed\n$completed"

        // Pie chart
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(completed.toFloat(), "Done"))
        entries.add(PieEntry((total - completed).coerceAtLeast(0).toFloat(), "Pending"))

        val set = PieDataSet(entries, "")
        set.colors = listOf(
            resources.getColor(R.color.chartCompleted), // Dark green
            resources.getColor(R.color.chartPending)    // Orange
        )
        set.valueTextSize = 14f
        set.valueTextColor = Color.WHITE

        val data = PieData(set)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.invalidate()

        // ✅ Line chart (today's progress: Completed vs Pending)
        val lineEntries = ArrayList<Entry>()
        lineEntries.add(Entry(0f, completed.toFloat()))
        lineEntries.add(Entry(1f, (total - completed).coerceAtLeast(0).toFloat()))

        val lineSet = LineDataSet(lineEntries, "Today's Progress")
        lineSet.color = Color.BLACK
        lineSet.lineWidth = 3f
        lineSet.setCircleColor(Color.DKGRAY)
        lineSet.circleRadius = 5f
        lineSet.valueTextSize = 12f
        lineSet.valueTypeface = Typeface.DEFAULT_BOLD
        lineSet.valueTextColor = Color.BLACK

        val lineData = LineData(lineSet)
        lineChart.data = lineData

        // X-axis labels
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Completed", "Pending"))
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.BLACK
        xAxis.typeface = Typeface.DEFAULT_BOLD

        // Y-axis
        lineChart.axisLeft.textColor = Color.BLACK
        lineChart.axisRight.isEnabled = false

        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.animateX(800)
        lineChart.invalidate()

        // 💧 Hydration section
        val hydrationTitle = findViewById<TextView>(R.id.tvHydrationTitle)
        val hydrationStatus = findViewById<TextView>(R.id.tvHydrationStatus)

        val interval = prefs.loadString("hydration_interval", "Not set")
        val nextTime = prefs.loadString("next_hydration_time", "No reminder scheduled")

        hydrationTitle.text = "💧 Hydration Reminder"
        hydrationStatus.text = "Interval: $interval\n\nNext Reminder: $nextTime"




    }
}
