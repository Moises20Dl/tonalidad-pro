package com.example.dsp

import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class PitchResult(
    val frequencyHz: Float,
    val clarity: Float,
    val midiNote: Int,
    val cents: Float,
    val noteLetter: String,
    val noteSolfa: String,
    val octave: Int
)

object YinPitchTracker {
    private val NOTE_LETTERS = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val NOTE_SOLFA = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")

    fun freqToMidi(f: Float, refA4: Float = 440f): Float {
        if (f <= 0f) return 0f
        return 69f + 12f * (log2(f / refA4))
    }

    fun midiToFreq(midi: Float, refA4: Float = 440f): Float {
        return refA4 * (2.0.pow((midi - 69.0) / 12.0)).toFloat()
    }

    fun getPitchResult(frequency: Float, clarity: Float, refA4: Float = 440f): PitchResult {
        val midiContinuous = freqToMidi(frequency, refA4)
        val midiInt = midiContinuous.roundToInt()
        val cents = (midiContinuous - midiInt) * 100f
        val pitchClass = ((midiInt % 12) + 12) % 12
        val octave = (midiInt / 12) - 1

        return PitchResult(
            frequencyHz = frequency,
            clarity = clarity,
            midiNote = midiInt,
            cents = cents,
            noteLetter = NOTE_LETTERS[pitchClass],
            noteSolfa = NOTE_SOLFA[pitchClass],
            octave = octave
        )
    }

    /**
     * YIN pitch estimation algorithm
     * @param buffer audio samples (float normalized -1.0 to 1.0)
     * @param sampleRate sampling rate in Hz (e.g., 44100)
     * @param threshold YIN absolute threshold (typically 0.12f)
     * @return Pair<frequencyHz, clarity> or null if no pitched sound detected
     */
    fun process(buffer: FloatArray, sampleRate: Int, threshold: Float = 0.12f): Pair<Float, Float>? {
        val half = buffer.size / 2
        if (half < 10) return null

        val minTau = max(2, sampleRate / 1200) // ~1200 Hz max
        val maxTau = min(half - 1, sampleRate / 60) // ~60 Hz min (e.g. low B1)
        if (minTau >= maxTau) return null

        // Step 1: Difference function
        val diff = FloatArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0f
            for (i in 0 until half) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference function (CMND)
        val cmnd = FloatArray(maxTau + 1)
        cmnd[0] = 1f
        var runningSum = 0f
        for (tau in 1..maxTau) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum == 0f) 1f else (diff[tau] * tau) / runningSum
        }

        // Step 3: Absolute threshold
        var tau = -1
        for (t in minTau..maxTau) {
            if (cmnd[t] < threshold) {
                var localMin = t
                while (localMin + 1 <= maxTau && cmnd[localMin + 1] < cmnd[localMin]) {
                    localMin++
                }
                tau = localMin
                break
            }
        }

        if (tau < 0) return null

        // Step 4: Parabolic interpolation
        var betterTau = tau.toFloat()
        if (tau > 1 && tau < maxTau) {
            val a = cmnd[tau - 1]
            val b = cmnd[tau]
            val c = cmnd[tau + 1]
            val den = 2f * (2f * b - a - c)
            if (den != 0f) {
                betterTau = tau + (c - a) / den
            }
        }

        if (betterTau <= 0f) return null
        val freq = sampleRate / betterTau
        val clarity = (1f - cmnd[tau]).coerceIn(0f, 1f)
        return Pair(freq, clarity)
    }
}
