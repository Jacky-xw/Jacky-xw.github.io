package com.bodhi.daily

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this)
        text.text = "菩提日课 v2.0.1\n\n今日修行：\n□ 戒\n□ 定\n□ 慧\n□ 念佛\n□ 回向"
        text.textSize = 20f
        setContentView(text)
    }
}
