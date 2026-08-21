package com.chiranjeevankumar.aura

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = "AURA\n\nAndroid Bridge v0.1.0\n\nONLINE"
            textSize = 26f
            setPadding(40, 80, 40, 40)
        }

        setContentView(text)
    }
}
