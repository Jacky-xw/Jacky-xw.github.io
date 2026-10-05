package com.ruru.practice.feature.meditation

import android.content.Context

/**
 * Deprecated path: end UI is the single FGS notification in [MeditationKeepAliveService].
 * Methods kept as no-ops so older call sites compile without posting a second notification.
 */
object SessionEndNotifier {
    fun ensureChannel(context: Context) = Unit
    fun postEndAlert(context: Context, longSeat: Boolean, withSound: Boolean = false) = Unit
    fun cancel(context: Context) = Unit
}
