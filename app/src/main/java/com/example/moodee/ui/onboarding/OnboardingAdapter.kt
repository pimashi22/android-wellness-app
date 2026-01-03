package com.example.moodee.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.moodee.R

class OnboardingAdapter(
    private val activity: OnboardingActivity,
    private val viewPager: ViewPager2
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val images = listOf(
        R.drawable.onboard1,
        R.drawable.onboard2,
        R.drawable.onboard3
    )

    private val titles = listOf(
        "Track Your Habits",
        "Log Your Mood",
        "Stay Hydrated"
    )

    private val subtitles = listOf(
        "Build consistency with daily routines.",
        "Capture your feelings with emojis.",
        "Get reminders to drink water regularly."
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(images[position], titles[position], subtitles[position], position)
    }

    override fun getItemCount() = images.size

    inner class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.onboardImage)
        private val title: TextView = itemView.findViewById(R.id.onboardTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.onboardSubtitle)
        private val btnNext: Button = itemView.findViewById(R.id.btnNext)
        private val btnSkip: Button = itemView.findViewById(R.id.btnSkip)

        fun bind(img: Int, t: String, st: String, position: Int) {
            image.setImageResource(img)
            title.text = t
            subtitle.text = st

            // Change button text on last page
            btnNext.text = if (position == images.size - 1) "Get Started" else "Next"

            // Next button → move forward or finish
            btnNext.setOnClickListener {
                if (position == images.size - 1) {
                    activity.navigateToHome()
                } else {
                    viewPager.currentItem = position + 1
                }
            }

            // Skip → go straight to Home
            btnSkip.setOnClickListener {
                activity.navigateToHome()
            }
        }
    }
}
