package com.OnTime.ontime.ui

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.OnTime.ontime.R

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var transportModeGroup: RadioGroup
    private lateinit var notificationToneSwitch: Switch
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        transportModeGroup = findViewById(R.id.transport_mode_group)
        notificationToneSwitch = findViewById(R.id.notification_tone_switch)
        
        setupListeners()
        loadSettings()
    }
    
    private fun setupListeners() {
        transportModeGroup.setOnCheckedChangeListener { _, checkedId ->
            // TODO: Save transport mode preference
        }
        
        notificationToneSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save notification tone preference (강함/약함)
        }
    }
    
    private fun loadSettings() {
        // TODO: Load user preferences
    }
}
