package com.beackers.pass.alarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder
import android.content.Intent
import android.content.Context

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch

import com.beackers.pass.databinding.ActivityAlarmBinding

class AlarmActivity : AppCompatActivity() {
  private lateinit var uiBinding: ActivityAlarmBinding

  private var alarmService: AlarmService? = null
  private var isBound = false
  private val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      val binder = service as AlarmService.LocalBinder
      alarmService = binder.getService()
      isBound = true
      observeAlarmStates()
    }
    override fun onServiceDisconnected(name: ComponentName?) {
      isBound = false
      alarmService = null
    }
  }

  override fun onStart() {
    super.onStart()
    startService(Intent(this, AlarmService::class.java))
    Intent(this, AlarmService::class.java).also { intent ->
      bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
  }
  
  override fun onStop() {
    super.onStop()
    if (isBound) {
      unbindService(connection)
      isBound = false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    uiBinding = ActivityAlarmBinding.inflate(layoutInflater)
    setContentView(uiBinding.root)
  }

  private fun observeAlarmStates() {
    val service = alarmService ?: return
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        service.uiState.collect { state ->
          updateUi(state)
        }
      }
    }
  }

  private fun updateUi(state: AlarmService.PassUiState) {
    // update ui things here
    // TODO
  }
}
