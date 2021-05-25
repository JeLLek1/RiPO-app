package com.bronikowski.ripo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var acceptButton : Button
    private lateinit var soundSwitch : Switch
    private lateinit var cascadeTypeToggle : ToggleButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        acceptButton = findViewById(R.id.acceptButton)
        soundSwitch = findViewById(R.id.soundSwitch)
        cascadeTypeToggle = findViewById(R.id.cascadeTypeToggle)

        acceptButton.setOnClickListener{
            val intent = Intent(this@MainActivity, CameraActivity::class.java)
            intent.putExtra("soundEffects", soundSwitch.isChecked())
            intent.putExtra("cascadeType", cascadeTypeToggle.isChecked())
            startActivity(intent)
        }
    }
}