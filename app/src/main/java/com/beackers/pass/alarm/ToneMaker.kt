package com.beackers.pass.alarm

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.*

class ToneMaker {

    private val sampleRate = 44100
    private val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        AudioTrack.MODE_STREAM
    )

    init {
        audioTrack.play()
    }

    fun writeTone(freqHz: Double, durationMs: Int, volume: Double = 0.9) {
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)

        for (i in 0 until count) {
            val t = i / sampleRate.toDouble()
            val sample =
                if (sin(2.0 * Math.PI * freqHz * t) > 0) 1.0 else -1.0

            buffer[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
        }

        audioTrack.write(buffer, 0, buffer.size)
    }

    fun stop() {
        audioTrack.stop()
        audioTrack.release()
    }
}
