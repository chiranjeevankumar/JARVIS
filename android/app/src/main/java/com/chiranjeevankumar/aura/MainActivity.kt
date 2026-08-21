package com.chiranjeevankumar.aura

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = TextView(this)

        view.text = "AURA\n\nAndroid Bridge v0.1.2\n\nONLINE"
        view.textSize = 24f
        view.setPadding(40, 80, 40, 40)

        setContentView(view)
    }
}
