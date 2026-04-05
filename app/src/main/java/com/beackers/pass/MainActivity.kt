package com.beackers.dumbhome

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.provider.Settings
import android.provider.MediaStore
import android.content.Context
import android.content.IntentFilter
import android.text.TextUtils

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    /* background thread operations

    private val thingRunnable = object : Runnable {
      override fun run() {
        handler.postDelayed(this, delay)
      }
    }

    to stop: handler.removeCallbacks(clockRunnable)
    to restart: handler.post(clockRunnable)
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}
