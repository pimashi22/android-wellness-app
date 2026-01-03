package com.example.moodee.ui.habits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moodee.R
import com.example.moodee.model.Habit

class HabitsAdapter(
    private val items: MutableList<Habit>,
    private val onToggle: (pos: Int) -> Unit,
    private val onEdit: (pos: Int) -> Unit,
    private val onDelete: (pos: Int) -> Unit
) : RecyclerView.Adapter<HabitsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitsViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitsViewHolder(v)
    }

    override fun onBindViewHolder(holder: HabitsViewHolder, position: Int) {
        val h = items[position]
        holder.bind(h)

        // Clear previous listener to avoid multiple triggers when recycling
        holder.chkDone.setOnCheckedChangeListener(null)
        holder.chkDone.isChecked = h.doneToday

        holder.chkDone.setOnCheckedChangeListener { _, isChecked ->
            h.doneToday = isChecked          // update model immediately
            onToggle(position)               // let activity persist & refresh home summary
        }

        holder.btnEdit.setOnClickListener { onEdit(position) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
    }

    override fun getItemCount(): Int = items.size
}
