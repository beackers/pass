package com.beackers.pass.alarm

import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.IBinder
import android.os.Binder
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

import androidx.core.app.NotificationCompat

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlarmService : Service() {
  // scope declaration
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  // PASS state management
  sealed class Event {
    object Arm : Event()
    object Disarm : Event()
    object Tick : Event()
    object Motion : Event()
  }
  private val eventChannel = Channel<Event>(Channel.UNLIMITED)
  private var previousState: AlarmState = AlarmState.STANDBY
  private var currentState: AlarmState = AlarmState.STANDBY
  private var pressureWindow = ArrayDeque<Float>()
  private var lastPressureRange = 0
  private const val WINDOW_SIZE = 25
  private const val MOVEMENT_THRESHOLD = 0.08f
  private lateinit var sensorManager: SensorManager
  private var pressureSensor: Sensor? = null
  private var pressureListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
      val pressure = event.values[0]
      handlePressure(pressure)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
  }

  // timer
  private var idleSeconds = 0

  // alarm
  private var alarmNoise = AlarmNoise()

  // ui updates
  data class PassUiState(
    val alarmState: AlarmState = AlarmState.STANDBY,
    val idleSeconds: Int = 0,
    val pressureRange: Float = 0f
  )
  private val _uiState = MutableStateFlow(PassUiState())
  val uiState: StateFlow<PassUiState> = _uiState
  inner class LocalBinder : Binder() {
    fun getService(): AlarmService = this@AlarmService
  }

  private var binder = LocalBinder()
  
  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    startForeground(1, buildNotification())
    startStateMachine() // set as STANDBY and wait for arming
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    if (pressureSensor != null) {
      sensorManager.registerListener(
        pressureListener,
        pressureSensor,
        200_000
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel() // clear timers and alarms
    sensorManager.unregisterListener(pressureListener)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_ARM -> eventChannel.trySend(Event.Arm)
      ACTION_DISARM -> eventChannel.trySend(Event.Disarm)
      ACTION_RESET -> eventChannel.trySend(Event.Motion)
    }
    return START_STICKY
  }

  private fun buildNotification(): Notification {
    val channelId = "pass_channel"

    val channel = NotificationChannel(
      channelId,
      "PASS Alarm",
      NotificationManager.IMPORTANCE_HIGH
    )
    getSystemService(NotificationManager::class.java)
      .createNotificationChannel(channel)
    return NotificationCompat.Builder(this, channelId)
      .setContentTitle("PASS Armed")
      .setContentText("Monitoring movement")
      .build()
  }

  // STATE MACHINE

  private fun startStateMachine() {
    serviceScope.launch {
      currentState = AlarmState.STANDBY

      while (isActive) {
        val event = eventChannel.receive()

        when (event) {
          Event.Arm -> idleSeconds = 0
          Event.Disarm -> {}
          Event.Motion -> idleSeconds = 0
          Event.Tick -> idleSeconds++
        }

        val newstate = reduce(currentState, event)

        if (newstate != currentState) {
          previousState = currentState
          currentState = newstate
          onStateChanged(previousState, currentState)
        }

        publishUiState()
      }
    }
  }

  private fun reduce(state: AlarmState, event: Event): AlarmState {
    when (state) {
      AlarmState.STANDBY -> when (event) {
        Event.Arm -> AlarmState.ARMED
        else -> state
      }

      AlarmState.ARMED -> when (event) {
        Event.Disarm -> AlarmState.STANDBY
        Event.Tick -> {
          if (idleSeconds >= 20) AlarmState.PREALARM
          else state
        }
        else -> state
      }

      AlarmState.PREALARM -> when (event) {
        Event.Tick -> when {
          idleSeconds >= 30 -> AlarmState.ALARM
          idleSeconds < 20 -> AlarmState.ARMED
        }
        Event.Disarm -> AlarmState.STANDBY
        Event.Motion -> AlarmState.ARMED
        else -> state
      }

      AlarmState.ALARM -> when (event) {
        Event.Tick -> {
          if (idleSeconds < 20) AlarmState.ARMED
          else state
        }
        Event.Motion -> AlarmState.ARMED
        Event.Disarm -> AlarmState.STANDBY
        else -> state
      }
    }
  }

  private fun onStateChanged(old: AlarmState, new: AlarmState) {
    when (new) {
      AlarmState.STANDBY -> alarmNoise.stop()
      AlarmState.ARMED -> {
        alarmNoise.stop()
        alarmNoise.ack()
      }
      AlarmState.PREALARM -> {
        alarmNoise.stop()
        alarmNoise.prealarm()
      }
      AlarmState.ALARM -> {
        alarmNoise.stop()
        alarmNoise.alarm()
      }
  }

  private fun handlePressure(pressure: Float) {
    pressureWindow.addLast(pressure)
    if (pressureWindow.size > WINDOW_SIZE) {
      pressureWindow.removeFirst()
    }
    if (pressureWindow.size == WINDOW_SIZE) {
      val min = pressureWindow.minOrNull()!!
      val max = pressureWindow.maxOrNull()!!
      val range = max - min
      lastPressureRange = range

      if (range > MOVEMENT_THRESHOLD) {
        eventChannel.trySend(Event.Motion)
      }
    }
  }

  // Update timer
  private fun startTimer() {
    serviceScope.launch {
      while (isActive) {
        delay(1000)
        eventChannel.trySend(Event.Tick)
      }
    }
  }

  private suspend fun publishUiState() {
    _uiState.value = PassUiState(
      alarmState = currentState,
      idleSeconds = idleSeconds,
      pressureRange = lastPressureRange
    )
  }
}
