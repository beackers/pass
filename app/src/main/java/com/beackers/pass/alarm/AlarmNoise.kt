package com.beackers.pass.alarm

import kotlinx.coroutines.* 

class AlarmNoise {
  private val alarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val tone = ToneMaker()

  fun stop() {
    alarmScope.cancel()
  }

  fun ack() {
    alarmScope.launch {
      repeat(3) {
        tone.writeTone(3400, 90)
        delay(90)
      }
    }
  }

  fun prealarm() {
    alarmScope.launch {
    }
  }

  fun alarm() {
    alarmScope.launch {
    }
  }
}
