package com.ruru.practice.feature.practice

import com.ruru.practice.feature.meditation.MeditationKeepAliveService

/** Separate service instance: walking must not replace a meditation deadline. */
class WalkingKeepAliveService : MeditationKeepAliveService() {
    override val walking: Boolean = true
}
