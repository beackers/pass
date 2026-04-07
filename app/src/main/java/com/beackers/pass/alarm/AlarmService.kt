package com.beackers.pass.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

import androidx.core.app.NotificationCompat

class AlarmService : Service() {

  private var state = AlarmState.STANDBY

  override fun onBind(intent: Intent?) = null

  override fun onCreate() {
    super.onCreate()
    startForeground(1, buildNotification())
    // TODO: start barometer window and timers
  }

  override fun onDestroy() {
    super.onDestroy()
    // TODO: clean up timers
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
    // TODO startTimer()
    // TODO AlarmNoise.ack()
  }

  private fun disarm() {
    state = AlarmState.STANDBY
    // TODO stopTimer()
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
      // TODO resetTimer()
      // TODO AlarmSound.stop()
      // TODO AlarmSound.ack()
    }
  }
}
