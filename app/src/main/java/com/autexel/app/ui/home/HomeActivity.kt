package com.autexel.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.autexel.app.R
import com.autexel.app.databinding.ActivityHomeBinding
import com.autexel.app.ui.camera.CameraActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    companion object {
        const val MODE_EXCEL = "EXCEL"
        const val MODE_INVOICE = "INVOICE"
        const val EXTRA_MODE = "SCAN_MODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateCards()
        setupClickListeners()
    }

    private fun animateCards() {
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUp.duration = 400
        binding.cardExcel.startAnimation(slideUp)

        val slideUp2 = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUp2.duration = 550
        binding.cardInvoice.startAnimation(slideUp2)
    }

    private fun setupClickListeners() {
        binding.cardExcel.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                    openCamera(MODE_EXCEL)
                }.start()
            }.start()
        }

        binding.cardInvoice.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                    openCamera(MODE_INVOICE)
                }.start()
            }.start()
        }
    }

    private fun openCamera(mode: String) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
        }
        startActivity(intent)
    }
}
