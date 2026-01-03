package com.example.moodee.receivers

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moodee.R
import com.example.moodee.ui.habits.HabitsActivity
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "moodee_notifications"
        const val CHANNEL_NAME = "Moodee Notifications"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "Reminder"
        val text = intent?.getStringExtra("text") ?: "It's time!"
        val habitId = intent?.getLongExtra("habit_id", -1L) ?: -1L
        val frequency = intent?.getStringExtra("frequency") ?: "Daily"
        val hour = intent?.getIntExtra("hour", 0) ?: 0
        val minute = intent?.getIntExtra("minute", 0) ?: 0

        Log.d(TAG, "onReceive() : title=$title habitId=$habitId freq=$frequency time=$hour:$minute")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // use default system sound
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // ensures heads-up popup
            ).apply {
                description = "Reminders for your daily habits"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 250, 400)
                setSound(soundUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            nm.createNotificationChannel(channel)
        }


        // PendingIntent to open HabitsActivity when user taps the notification
        val tapIntent = Intent(context, HabitsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingTap = PendingIntent.getActivity(context, 0, tapIntent, tapFlags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // make sure this exists
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // ✅ sound, vibrate, lights
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        nm.notify(((System.currentTimeMillis() % 10000).toInt()), builder.build())

        // Reschedule the next occurrence (Daily / Weekly / specific day / weekdays/weekends)
        try {
            scheduleNext(context, habitId, frequency, hour, minute, title, text)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next alarm", e)
        }
    }

    private fun scheduleNext(context: Context, habitId: Long, frequency: String, hour: Int, minute: Int, title: String, text: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // if alarm time already passed for today -> advance according to frequency
        if (nextCal.timeInMillis <= System.currentTimeMillis()) {
            when (frequency.lowercase(Locale.US)) {
                "daily" -> nextCal.add(Calendar.DAY_OF_MONTH, 1)
                "weekly" -> nextCal.add(Calendar.DAY_OF_MONTH, 7)
                "weekdays" -> {
                    // move to next weekday
                    do {
                        nextCal.add(Calendar.DAY_OF_MONTH, 1)
                    } while (nextCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || nextCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                "weekends" -> {
                    // move to next weekend day
                    do {
                        nextCal.add(Calendar.DAY_OF_MONTH, 1)
                    } while (nextCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && nextCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                }
                else -> {
                    // try day name: monday, tuesday...
                    val dow = dayNameToCalendarConst(frequency)
                    if (dow != -1) {
                        val todayDow = nextCal.get(Calendar.DAY_OF_WEEK)
                        var diff = (dow - todayDow + 7) % 7
                        if (diff == 0) diff = 7
                        nextCal.add(Calendar.DAY_OF_MONTH, diff)
                    } else {
                        // fallback daily
                        nextCal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
        }

        val resIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
            putExtra("habit_id", habitId)
            putExtra("frequency", frequency)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val requestCode = (habitId % Int.MAX_VALUE).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, requestCode, resIntent, flags)

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pending)
            Log.d(TAG, "Rescheduled next alarm for habitId=$habitId at ${nextCal.time}")
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException when rescheduling exact alarm; falling back to set()", se)
            am.set(AlarmManager.RTC_WAKEUP, nextCal.timeInMillis, pending)
            Log.d(TAG, "Rescheduled (non-exact) alarm for habitId=$habitId at ${nextCal.time}")
        }
    }

    private fun dayNameToCalendarConst(name: String?): Int {
        if (name == null) return -1
        return when (name.lowercase(Locale.US)) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> -1
        }
    }
}
