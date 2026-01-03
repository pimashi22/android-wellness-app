package com.example.moodee.ui.habits

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.moodee.R
import com.example.moodee.model.Habit

class HabitsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvTitle: TextView = itemView.findViewById(R.id.tvHabitTitle)
    val chkDone: AppCompatCheckBox = itemView.findViewById(R.id.chkDone)
    val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    private val tvDetails: TextView = itemView.findViewById(R.id.tvHabitDetails)

    fun bind(h: Habit) {
        tvTitle.text = h.title
        tvDetails.text = "${h.frequency} • %02d:%02d".format(h.timeHour, h.timeMinute)
    }
}

