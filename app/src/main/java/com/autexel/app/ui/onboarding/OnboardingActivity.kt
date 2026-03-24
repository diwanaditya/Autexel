package com.autexel.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.autexel.app.R
import com.autexel.app.ui.home.HomeActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREF = "autexel_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_complete"

        fun shouldShow(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            return !prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        }

        fun markDone(context: Context) {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        }
    }

    data class OnboardPage(val title: String, val description: String, val icon: String)

    private val pages = listOf(
        OnboardPage(
            "Welcome to Autexel",
            "Convert handwritten notes into digital files.\nNo internet needed. Everything stays on your device.",
            "[A]"
        ),
        OnboardPage(
            "Scan to Excel",
            "Point your camera at any handwritten table, list, or notes.\nAutexel detects the structure and builds a spreadsheet automatically.",
            "[XLS]"
        ),
        OnboardPage(
            "Scan to Invoice",
            "Scan handwritten bills or receipts.\nAutexel detects items, quantities and prices - then generates a professional PDF invoice.",
            "[INV]"
        ),
        OnboardPage(
            "Tips for Best Results",
            "- Use good lighting (natural light works best)\n- Hold camera 20-30cm from paper\n- Keep text horizontal and in frame\n- Dark ink on white paper gives best accuracy\n- You can also pick an image from your Gallery",
            "[!]"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)

        viewPager.adapter = OnboardingAdapter(pages)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnSkip.setOnClickListener { finishOnboarding() }

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnNext.text = if (position == pages.size - 1) "Get Started" else "Next"
                btnSkip.visibility = if (position == pages.size - 1) View.GONE else View.VISIBLE
            }
        })
    }

    private fun finishOnboarding() {
        markDone(this)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    class OnboardingAdapter(private val pages: List<OnboardPage>) :
        RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvOnboardIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvOnboardTitle)
            val tvDesc: TextView = view.findViewById(R.id.tvOnboardDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.tvIcon.text = pages[position].icon
            holder.tvTitle.text = pages[position].title
            holder.tvDesc.text = pages[position].description
        }

        override fun getItemCount() = pages.size
    }
}
