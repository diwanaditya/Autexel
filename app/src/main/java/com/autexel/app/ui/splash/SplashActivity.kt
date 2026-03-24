package com.autexel.app.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.autexel.app.databinding.ActivitySplashBinding
import com.autexel.app.ui.country.CountryPickerActivity
import com.autexel.app.ui.home.HomeActivity
import com.autexel.app.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system UI for full immersion
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        animateSplash()

        Handler(Looper.getMainLooper()).postDelayed({
            val dest = when {
                CountryPickerActivity.shouldShow(this) -> CountryPickerActivity::class.java
                OnboardingActivity.shouldShow(this)    -> OnboardingActivity::class.java
                else                                   -> HomeActivity::class.java
            }
            startActivity(Intent(this, dest))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }

    private fun animateSplash() {
        // Start invisible
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f
        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f

        // Logo animation
        val logoAlpha = ObjectAnimator.ofFloat(binding.ivLogo, "alpha", 0f, 1f).apply {
            duration = 700
        }
        val logoScaleX = ObjectAnimator.ofFloat(binding.ivLogo, "scaleX", 0.5f, 1f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
        }
        val logoScaleY = ObjectAnimator.ofFloat(binding.ivLogo, "scaleY", 0.5f, 1f).apply {
            duration = 700
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Text animations
        val nameAlpha = ObjectAnimator.ofFloat(binding.tvAppName, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 500
        }
        val nameTranslate = ObjectAnimator.ofFloat(binding.tvAppName, "translationY", 30f, 0f).apply {
            duration = 600
            startDelay = 500
        }
        val taglineAlpha = ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 800
        }

        AnimatorSet().apply {
            playTogether(logoAlpha, logoScaleX, logoScaleY, nameAlpha, nameTranslate, taglineAlpha)
            start()
        }
    }
}
