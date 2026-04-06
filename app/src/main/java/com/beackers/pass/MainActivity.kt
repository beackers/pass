package com.beackers.pass

import android.os.Bundle
import android.content.Intent

import androidx.appcompat.app.AppCompatActivity

import com.beackers.pass.databinding.ActivityMainBinding
import com.beackers.pass.alarm.AlarmActivity
import com.beackers.settings.SettingsActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.armButton.setOnClickListener {
          val intent = Intent(this, AlarmActivity::class.java)
          startActivity(intent)
        }

        binding.settingsButton.setOnClickListener {
          val intent = Intent(this, SettingsActivity::class.java)
          startActivity(intent)
        }
    }
}
