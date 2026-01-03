package com.example.moodee.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moodee.R
import java.util.*

class HydrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hydration_channel",
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "hydration_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💧 Hydration Reminder")
            .setContentText("Time to drink water!")
            .setAutoCancel(true)

        nm.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }
}
