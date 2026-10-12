package com.example.dsp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioToneGenerator {
    private const val SAMPLE_RATE = 44100
    private var continuousTrack: AudioTrack? = null
    private var isPlayingContinuous = false

    /**
     * Synthesizes and plays a single musical note with a natural acoustic ADSR envelope.
     */
    suspend fun playNote(frequencyHz: Float, durationMs: Long, volume: Float = 0.8f) {
        if (frequencyHz <= 0f || durationMs <= 0) return

        withContext(Dispatchers.Default) {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            if (numSamples <= 0) return@withContext

            val buffer = ShortArray(numSamples)
            val twoPiF = 2.0 * PI * frequencyHz / SAMPLE_RATE

            val attackSamples = (SAMPLE_RATE * 0.02f).toInt() // 20ms attack
            val releaseSamples = (SAMPLE_RATE * 0.05f).toInt() // 50ms release

            for (i in 0 until numSamples) {
                // Harmonic richness (fundamental + 2nd + 3rd harmonic for rich tone)
                val fundamental = sin(i * twoPiF)
                val h2 = 0.3 * sin(i * twoPiF * 2.0)
                val h3 = 0.15 * sin(i * twoPiF * 3.0)
                var raw = (fundamental + h2 + h3) / 1.45

                // Envelope
                val envelope = when {
                    i < attackSamples -> (i.toFloat() / attackSamples).toDouble()
                    i > numSamples - releaseSamples -> ((numSamples - i).toFloat() / releaseSamples).toDouble()
                    else -> 1.0 - 0.2 * (i.toDouble() / numSamples) // gentle decay
                }

                val sample = (raw * envelope * volume * Short.MAX_VALUE).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )
                buffer[i] = sample.toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                kotlinx.coroutines.delay(durationMs)
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Starts continuous tone playback (e.g., A4 440 Hz reference fork)
     */
    fun startContinuousTone(frequencyHz: Float, volume: Float = 0.7f) {
        stopContinuousTone()
        if (frequencyHz <= 0f) return

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        continuousTrack = track
        isPlayingContinuous = true
        track.play()

        Thread {
            val chunk = ShortArray(1024)
            var phase = 0.0
            val phaseInc = 2.0 * PI * frequencyHz / SAMPLE_RATE

            while (isPlayingContinuous) {
                for (i in chunk.indices) {
                    val s = (sin(phase) * volume * Short.MAX_VALUE).toInt()
                    chunk[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    phase += phaseInc
                    if (phase >= 2.0 * PI) phase -= 2.0 * PI
                }
                track.write(chunk, 0, chunk.size)
            }
        }.start()
    }

    fun stopContinuousTone() {
        isPlayingContinuous = false
        try {
            continuousTrack?.stop()
            continuousTrack?.release()
        } catch (_: Exception) {}
        continuousTrack = null
    }
}
