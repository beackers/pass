package com.beackers.pass.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

import androidx.core.app.NotificationCompat

import kotlinx.coroutines.*

class AlarmService : Service() {
  // scope declaration
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  // PASS state management
  //private var measureJob: Job? = null
  private var state = AlarmState.STANDBY
  private var pressureWindow = ArrayDeque<Float>()
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
  
  override fun onBind(intent: Intent?) = null

  override fun onCreate() {
    super.onCreate()
    startForeground(1, buildNotification())
    startTimer() // start timer, regardless
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
    serviceScope.cancel() // clear timers
    sensorManager.unregisterListener(pressureListener)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_ARM -> arm()
      ACTION_DISARM -> disarm()
      ACTION_PREALARM -> prealarm()
      ACTION_ALARM -> alarm()
      ACTION_RESET -> reset()
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
      // .setSmallIcon(R.drawable.ic_launcher_foreground)
      .build()
  }

  private fun arm() {
    if (state != AlarmState.STANDBY) return
    state = AlarmState.ARMED
    // TODO AlarmNoise.ack()
  }

  private fun disarm() {
    state = AlarmState.STANDBY
    // TODO AlarmSound.stop()
  }

  private fun prealarm() {
    if (state != AlarmState.ARMED) return
    state = AlarmState.PREALARM
    // TODO AlarmSound.prealarm()
  }

  private fun alarm() {
    if (state != AlarmState.PREALARM) return
    state = AlarmState.ALARMING
    // TODO AlarmSound.alarm()
  }

  private fun reset() {
    if (state == AlarmState.PREALARM || state == AlarmState.ALARMING) {
      state = AlarmState.ARMED
      registerMovement()
      // TODO AlarmSound.stop()
      // TODO AlarmSound.ack()
    }
  }


  // count until death
  private fun startTimer() {
    if (timerJob?.isActive == true) return
    seconds = 0
    timerJob = serviceScope.launch {
      while (isActive) {
        delay(1000)
        idleSeconds++
        onTick()
      }
    }
  }

  // reset if moved
  fun registerMovement() {
    idleSeconds = 0
  }

  // tick -> check if he's down
  private suspend fun onTick() {
    withContext(Dispatchers.Main) {
      when {
        idleSeconds == 20 && state == AlarmState.ARMED -> prealarm()
        idleSeconds == 30 && state == AlarmState.PREALARM -> alarm()
      }
    }
  }

  private fun handlePressure(pressure: Float) {
    if (state == AlarmState.STANDBY) return
    pressureWindow.addLast(pressure)
    if (pressureWindow.size > WINDOW_SIZE) {
      pressureWindow.removeFirst()
    }
    if (pressureWindow.size == WINDOW_SIZE) {
      val min = pressureWindow.minOrNull()!!
      val max = pressureWindow.maxOrNull()!!
      val range = max - min

      if (range > MOVEMENT_THRESHOLD) {
        registerMovement()
      }
    }
  }
}
