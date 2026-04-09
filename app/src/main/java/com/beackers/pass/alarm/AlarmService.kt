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
  private var current: AlarmState = AlarmState.STANDBY
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
  private var timerJob: Job? = null
  private var idleSeconds = 0

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
  
  override fun onBind(intent: Intent?) {
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    startForeground(1, buildNotification())
    startTimer() // start timer, regardless
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
      current = AlarmState.STANDBY

      while (isActive) {
        current = when (current) {
          AlarmState.STANDBY -> handleStandby()
          AlarmState.ARMED -> handleArmed()
          AlarmState.PREALARM -> handlePrealarm()
          AlarmState.ALARM -> handleAlarm()
        }
      }
    }
  }

  private suspend fun handleStandby(): AlarmState {
    while (true) {
      when (eventChannel.receive()) {
        Event.Arm -> return AlarmState.ARMED
      }
    }
  }

  private suspend fun handleArmed(): AlarmState {
    // TODO: AlarmNoise.stop()
    // TODO: AlarmNoise.ack()
    while (true) {
      when (eventChannel.receive()) {
        Event.Disarm -> return AlarmState.STANDBY
        Event.Motion -> {}
        Event.Tick -> if (idleSeconds >= 20) return AlarmState.PREALARM
        else -> {}
      }
    }
  }

  private suspend fun handlePrealarm(): AlarmState {
    // TODO: AlarmNoise.startPreAlarm()
    while (true) {
      when (eventChannel.receive()) {
          Event.Tick -> {
            if (idleSeconds >= 30) return AlarmState.ALARM
            if (idleSeconds < 20) return AlarmState.ARMED
          }
          Event.Motion -> return AlarmState.ARMED
          Event.Disarm -> return AlarmState.STANDBY
      }
    }
  }

  private suspend fun handleAlarm(): AlarmState {
    // TODO: AlarmNoise.startAlarm()
    while (true) {
      when (eventChannel.receive()) {
        Event.Tick -> if (idleSeconds < 20) return AlarmState.ARMED
        Event.Motion -> return AlarmState.ARMED
        Event.Disarm -> return AlarmState.STANDBY
        else -> {}
      }
    }
  }

  // TIMER

  // count until death
  private fun startTimer() {
    if (timerJob?.isActive == true) return
    timerJob = serviceScope.launch {
      while (isActive) {
        delay(1000)
        eventChannel.trySend(Event.Tick)
      }
    }
  }

  // reset if moved
  fun registerMovement() {
    eventChannel.trySend(Event.Motion)
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
        registerMovement()
      }
    }
  }

  // Update timer
  private fun startUiReducer() {
    serviceScope.launch {
      while (true) {
        when (eventChannel.receive()) {
          Event.Arm -> idleSeconds = 0
          Event.Tick -> idleSeconds++
          Event.Motion -> idleSeconds = 0
          else -> {}
        }
        // since we're reacting to events
        // push state anyways
        publishUiState()
      }
    }
  }

  private suspend fun publishUiState() {
    _uiState.value = PassUiState(
      alarmState = current,
      idleSeconds = idleSeconds,
      pressureRange = lastPressureRange
    )
  }
}
