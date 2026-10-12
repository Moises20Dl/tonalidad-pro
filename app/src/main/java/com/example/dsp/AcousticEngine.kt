package com.example.dsp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AcousticState(
    val isRunning: Boolean = false,
    val isCalibrating: Boolean = false,
    val calibrationCountdownSec: Int = 0,
    val calibrationStatus: String = "Micrófono inactivo",
    val noiseGateThresholdDb: Float = -50f,
    val currentRmsDb: Float = -100f,
    val normalizedRms: Float = 0f,
    val normalizedThreshold: Float = 0.1f,
    val sensitivity: Float = 2.5f,
    val referenceA4: Float = 440f,
    val currentPitch: PitchResult? = null,
    val smoothedCents: Float = 0f,
    val isInTune: Boolean = false,
    val chromaVector: FloatArray = FloatArray(12),
    val detectedKeys: List<KeyCandidate> = emptyList(),
    val totalPitchedFrames: Long = 0L,
    val notationMode: NotationMode = NotationMode.LETTERS
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AcousticState

        if (isRunning != other.isRunning) return false
        if (isCalibrating != other.isCalibrating) return false
        if (calibrationCountdownSec != other.calibrationCountdownSec) return false
        if (calibrationStatus != other.calibrationStatus) return false
        if (noiseGateThresholdDb != other.noiseGateThresholdDb) return false
        if (currentRmsDb != other.currentRmsDb) return false
        if (normalizedRms != other.normalizedRms) return false
        if (normalizedThreshold != other.normalizedThreshold) return false
        if (sensitivity != other.sensitivity) return false
        if (referenceA4 != other.referenceA4) return false
        if (currentPitch != other.currentPitch) return false
        if (smoothedCents != other.smoothedCents) return false
        if (isInTune != other.isInTune) return false
        if (!chromaVector.contentEquals(other.chromaVector)) return false
        if (detectedKeys != other.detectedKeys) return false
        if (totalPitchedFrames != other.totalPitchedFrames) return false
        if (notationMode != other.notationMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isRunning.hashCode()
        result = 31 * result + isCalibrating.hashCode()
        result = 31 * result + calibrationCountdownSec
        result = 31 * result + calibrationStatus.hashCode()
        result = 31 * result + noiseGateThresholdDb.hashCode()
        result = 31 * result + currentRmsDb.hashCode()
        result = 31 * result + normalizedRms.hashCode()
        result = 31 * result + normalizedThreshold.hashCode()
        result = 31 * result + sensitivity.hashCode()
        result = 31 * result + referenceA4.hashCode()
        result = 31 * result + (currentPitch?.hashCode() ?: 0)
        result = 31 * result + smoothedCents.hashCode()
        result = 31 * result + isInTune.hashCode()
        result = 31 * result + chromaVector.contentHashCode()
        result = 31 * result + detectedKeys.hashCode()
        result = 31 * result + totalPitchedFrames.hashCode()
        result = 31 * result + notationMode.hashCode()
        return result
    }
}

enum class NotationMode {
    LETTERS, SOLFEGE
}

class AcousticEngine(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(AcousticState())
    val state: StateFlow<AcousticState> = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var processingJob: Job? = null

    private val sampleRate = 44100
    private val bufferSize = 4096 // ~93ms window for accurate low bass YIN

    // Internal DSP thresholds
    private var baseNoiseFloorRms = 0.005f
    private var noiseGateRmsThreshold = 0.012f
    private var userSensitivity = 2.5f
    private var userRefA4 = 440f

    // Pitch filtering
    private var candidateMidi = -1
    private var candidateCount = 0
    private var missCount = 0
    private var smoothedCentsValue = 0f

    // Chroma vector accumulator
    private val accumulatedChroma = FloatArray(12)
    private var chromaWeight = 0f
    private var pitchedFramesCount = 0L

    init {
        recalculateThreshold()
    }

    private fun recalculateThreshold() {
        noiseGateRmsThreshold = max(0.004f, baseNoiseFloorRms * userSensitivity)
        val thresholdDb = if (noiseGateRmsThreshold > 0f) 20f * log10(noiseGateRmsThreshold) else -50f
        val normThresh = (sqrt(noiseGateRmsThreshold) * 2.2f).coerceIn(0f, 1f)
        _state.value = _state.value.copy(
            noiseGateThresholdDb = thresholdDb,
            normalizedThreshold = normThresh,
            sensitivity = userSensitivity,
            referenceA4 = userRefA4
        )
    }

    fun setSensitivity(sens: Float) {
        userSensitivity = sens.coerceIn(1.2f, 6.0f)
        recalculateThreshold()
    }

    fun setReferenceA4(ref: Float) {
        userRefA4 = ref.coerceIn(415f, 446f)
        recalculateThreshold()
    }

    fun setNotationMode(mode: NotationMode) {
        _state.value = _state.value.copy(notationMode = mode)
    }

    fun resetPitchTracker() {
        candidateMidi = -1
        candidateCount = 0
        missCount = 0
        smoothedCentsValue = 0f
        for (i in 0 until 12) accumulatedChroma[i] = 0f
        chromaWeight = 0f
        pitchedFramesCount = 0L
        _state.value = _state.value.copy(
            currentPitch = null,
            smoothedCents = 0f,
            isInTune = false,
            chromaVector = FloatArray(12),
            detectedKeys = emptyList(),
            totalPitchedFrames = 0L,
            calibrationStatus = if (_state.value.isRunning) "Buffer de tono reiniciado y limpio." else "Micrófono inactivo"
        )
    }

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (_state.value.isRunning) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val actualBufferSize = max(minBufferSize * 2, bufferSize * 2)

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                actualBufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                _state.value = _state.value.copy(
                    calibrationStatus = "Error: no se pudo inicializar la captura de audio"
                )
                return false
            }

            record.startRecording()
            audioRecord = record

            _state.value = _state.value.copy(
                isRunning = true,
                calibrationStatus = "Escuchando entrada de audio. Calibrá el silencio para máxima precisión."
            )

            startProcessingLoop()
            true
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                calibrationStatus = "Error al iniciar micrófono: ${e.message}"
            )
            false
        }
    }

    fun stopListening() {
        processingJob?.cancel()
        processingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        _state.value = _state.value.copy(
            isRunning = false,
            isCalibrating = false,
            currentPitch = null,
            smoothedCents = 0f,
            currentRmsDb = -100f,
            normalizedRms = 0f,
            calibrationStatus = "Micrófono desactivado"
        )
    }

    fun startCalibration() {
        if (!_state.value.isRunning) return
        scope.launch {
            _state.value = _state.value.copy(
                isCalibrating = true,
                calibrationCountdownSec = 4,
                calibrationStatus = "Calibrando silencio ambiental… guardá silencio total (4s)",
                currentPitch = null
            )

            val rmsSamples = mutableListOf<Float>()
            val startTime = System.currentTimeMillis()
            val durationMs = 4000L

            while (System.currentTimeMillis() - startTime < durationMs && _state.value.isCalibrating) {
                val elapsed = System.currentTimeMillis() - startTime
                val remainingSec = max(1, ((durationMs - elapsed) / 1000).toInt())
                _state.value = _state.value.copy(
                    calibrationCountdownSec = remainingSec,
                    calibrationStatus = "Midiendo ruido ambiente… $remainingSec s restantes (silencio)"
                )
                delay(250)
            }

            if (_state.value.isCalibrating) {
                val meanRms = if (rmsSamples.isNotEmpty()) rmsSamples.average().toFloat() else baseNoiseFloorRms
                val peakRms = if (rmsSamples.isNotEmpty()) rmsSamples.maxOrNull() ?: meanRms else meanRms
                baseNoiseFloorRms = max(meanRms, peakRms * 0.8f).coerceIn(0.002f, 0.08f)
                recalculateThreshold()

                val statusMessage = if (baseNoiseFloorRms > 0.035f) {
                    "Calibrado con aviso: nivel de ruido ambiental elevado (-${(-_state.value.noiseGateThresholdDb).toInt()} dB)."
                } else {
                    "Ambiente calibrado con éxito (-${(-_state.value.noiseGateThresholdDb).toInt()} dB). Listo para tocar o cantar."
                }

                _state.value = _state.value.copy(
                    isCalibrating = false,
                    calibrationCountdownSec = 0,
                    calibrationStatus = statusMessage
                )
            }
        }
    }

    private fun startProcessingLoop() {
        processingJob = scope.launch(Dispatchers.Default) {
            val shortBuffer = ShortArray(bufferSize)
            val floatBuffer = FloatArray(bufferSize)

            while (isActive && _state.value.isRunning) {
                val record = audioRecord ?: break
                val readCount = record.read(shortBuffer, 0, bufferSize)
                if (readCount <= 0) {
                    delay(10)
                    continue
                }

                // Compute RMS power
                var sumSq = 0f
                for (i in 0 until readCount) {
                    val f = shortBuffer[i] / 32768f
                    floatBuffer[i] = f
                    sumSq += f * f
                }
                val rms = sqrt(sumSq / readCount)
                val rmsDb = if (rms > 1e-5f) (20f * log10(rms)).coerceIn(-100f, 0f) else -100f
                val normRms = (sqrt(rms) * 2.2f).coerceIn(0f, 1f)

                // Calibration recording phase
                if (_state.value.isCalibrating) {
                    _state.value = _state.value.copy(
                        currentRmsDb = rmsDb,
                        normalizedRms = normRms
                    )
                    delay(15)
                    continue
                }

                // Noise Gate check
                if (rms < noiseGateRmsThreshold) {
                    missCount++
                    if (missCount > 4) {
                        candidateMidi = -1
                        candidateCount = 0
                        _state.value = _state.value.copy(
                            currentRmsDb = rmsDb,
                            normalizedRms = normRms,
                            currentPitch = null,
                            isInTune = false
                        )
                    } else {
                        _state.value = _state.value.copy(
                            currentRmsDb = rmsDb,
                            normalizedRms = normRms
                        )
                    }
                    delay(15)
                    continue
                }

                // Pitch Detection using YIN
                val yinResult = YinPitchTracker.process(floatBuffer, sampleRate, threshold = 0.12f)
                if (yinResult != null && yinResult.second >= 0.70f) {
                    missCount = 0
                    val freq = yinResult.first
                    val clarity = yinResult.second

                    val pitch = YinPitchTracker.getPitchResult(freq, clarity, userRefA4)

                    // Stability filter
                    if (pitch.midiNote == candidateMidi) {
                        candidateCount++
                    } else {
                        candidateMidi = pitch.midiNote
                        candidateCount = 1
                    }

                    // Smooth cents with low-pass filter
                    smoothedCentsValue = smoothedCentsValue * 0.4f + pitch.cents * 0.6f
                    val inTune = kotlin.math.abs(smoothedCentsValue) <= 4.5f

                    // Accumulate Chroma vector for tonality detection
                    val pc = ((pitch.midiNote % 12) + 12) % 12
                    accumulatedChroma[pc] = accumulatedChroma[pc] * 0.992f + 1.0f
                    for (i in 0 until 12) {
                        if (i != pc) accumulatedChroma[i] *= 0.992f
                    }
                    chromaWeight = chromaWeight * 0.992f + 1f
                    pitchedFramesCount++

                    val keys = if (pitchedFramesCount % 4L == 0L && chromaWeight > 5f) {
                        TonalityDetector.detectKeys(accumulatedChroma)
                    } else {
                        _state.value.detectedKeys
                    }

                    _state.value = _state.value.copy(
                        currentRmsDb = rmsDb,
                        normalizedRms = normRms,
                        currentPitch = pitch,
                        smoothedCents = smoothedCentsValue,
                        isInTune = inTune,
                        chromaVector = accumulatedChroma.copyOf(),
                        detectedKeys = keys,
                        totalPitchedFrames = pitchedFramesCount
                    )
                } else {
                    missCount++
                    if (missCount > 4) {
                        candidateMidi = -1
                        candidateCount = 0
                        _state.value = _state.value.copy(
                            currentRmsDb = rmsDb,
                            normalizedRms = normRms,
                            currentPitch = null,
                            isInTune = false
                        )
                    } else {
                        _state.value = _state.value.copy(
                            currentRmsDb = rmsDb,
                            normalizedRms = normRms
                        )
                    }
                }

                delay(15)
            }
        }
    }
}
