package com.example

import com.example.abc.AbcParser
import com.example.dsp.TonalityDetector
import com.example.dsp.YinPitchTracker
import com.example.etymology.EtymologyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ExampleUnitTest {

    @Test
    fun testYinPitchTrackerWithSynthesizedSine() {
        val sampleRate = 44100
        val buffer = FloatArray(4096)
        val targetFreq = 440.0 // A4

        // Generate 440Hz pure sine wave
        for (i in buffer.indices) {
            buffer[i] = sin(2.0 * PI * targetFreq * i / sampleRate).toFloat()
        }

        val result = YinPitchTracker.process(buffer, sampleRate, threshold = 0.15f)
        assertNotNull("YIN should detect 440Hz sine wave", result)
        val detectedFreq = result!!.first
        val clarity = result.second

        assertTrue("Clarity should be high for pure sine: $clarity", clarity > 0.85f)
        assertTrue("Detected freq should be close to 440Hz: $detectedFreq", kotlin.math.abs(detectedFreq - 440.0) < 3.0)

        val pitchResult = YinPitchTracker.getPitchResult(detectedFreq, clarity)
        assertEquals("A", pitchResult.noteLetter)
        assertEquals("La", pitchResult.noteSolfa)
        assertEquals(4, pitchResult.octave)
        assertEquals(69, pitchResult.midiNote)
    }

    @Test
    fun testTonalityDetectorCMajor() {
        // C Major diatonic triad weights: C (0), E (4), G (7)
        val chroma = FloatArray(12)
        chroma[0] = 6.0f // C
        chroma[4] = 4.0f // E
        chroma[7] = 5.0f // G
        chroma[2] = 1.0f // D
        chroma[5] = 1.0f // F
        chroma[9] = 1.0f // A
        chroma[11] = 1.5f // B

        val keys = TonalityDetector.detectKeys(chroma)
        assertTrue("Should detect 24 keys", keys.size == 24)
        val topKey = keys.first()

        assertEquals("Top key tonic should be C (pitch class 0)", 0, topKey.tonicPitchClass)
        assertEquals("Top key should be Major", false, topKey.isMinor)
        assertTrue("Correlation should be positive and strong", topKey.correlation > 0.5f)
    }

    @Test
    fun testAbcParserSampleScore() {
        val abc = """
            X:1
            T:Análisis de Terceras y Modulación
            M:4/4
            L:1/4
            Q:1/4=90
            K:C
            C E G c | _E G B _e | F A c f | _A c _e _a :|
        """.trimIndent()

        val score = AbcParser.parse(abc)
        assertEquals("Análisis de Terceras y Modulación", score.title)
        assertEquals("4/4", score.meter)
        assertEquals(90, score.tempoBpm)
        assertEquals("C", score.key)
        assertEquals(4, score.measures.size)

        // Measure 1: C E G c
        val m1 = score.measures[0]
        assertEquals(4, m1.notes.size)
        assertEquals('C', m1.notes[0].letter)
        assertEquals(4, m1.notes[0].octave)
        assertEquals('E', m1.notes[1].letter)
        assertEquals('G', m1.notes[2].letter)
        assertEquals('C', m1.notes[3].letter)
        assertEquals(5, m1.notes[3].octave)

        // Measure 2: _E G B _e (flat E)
        val m2 = score.measures[1]
        assertEquals(4, m2.notes.size)
        assertEquals('E', m2.notes[0].letter)
        assertEquals(com.example.abc.AccidentalType.FLAT, m2.notes[0].accidental)
    }

    @Test
    fun testEtymologyRepository() {
        val detailC4 = EtymologyRepository.getNoteHistory(60)
        assertEquals("Do", detailC4.solfa)
        assertEquals("C", detailC4.letter)
        assertEquals(4, detailC4.octave)
        assertTrue(detailC4.syllableOrigin.contains("Ut"))

        val detailA4 = EtymologyRepository.getNoteHistory(69)
        assertEquals("La", detailA4.solfa)
        assertEquals("A", detailA4.letter)
        assertTrue(detailA4.acousticCuriosity != null)

        val detailCSharp = EtymologyRepository.getNoteHistory(61)
        assertTrue(detailCSharp.accidentalHistory != null)
        assertTrue(detailCSharp.enharmonicName != null)
    }
}
