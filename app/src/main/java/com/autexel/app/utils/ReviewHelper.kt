package com.autexel.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Triggers Google Play in-app review after a successful export.
 * Google limits how often this can actually show — internally throttled.
 * Best triggered after a meaningful user action like first successful export.
 */
object ReviewHelper {

    private const val PREF_NAME = "autexel_prefs"
    private const val KEY_EXPORT_COUNT = "export_count"
    private const val REVIEW_THRESHOLD = 3 // Ask after 3 successful exports

    fun onExportSuccess(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_EXPORT_COUNT, 0) + 1
        prefs.edit().putInt(KEY_EXPORT_COUNT, count).apply()

        if (count == REVIEW_THRESHOLD) {
            requestReview(activity)
        }
    }

    private fun requestReview(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        // Review flow complete (user may or may not have reviewed)
                    }
                }
            }
        } catch (e: Exception) {
            // Fail silently — review is a bonus, not critical
        }
    }
}
