package com.ruru.practice.feature.meditation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ruru.practice.MainActivity

/**
 * Legacy entry; no longer used for alerts. Redirects into the main app.
 */
class SessionEndActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_LONG_SEAT = "long_seat"
        fun launch(context: Context, longSeat: Boolean) {
            // No-op: single notification path only.
        }
    }
}
