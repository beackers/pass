package com.beackers.pass.alarm

class AlarmService : Service() {
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
}
