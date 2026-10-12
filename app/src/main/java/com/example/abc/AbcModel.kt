package com.example.abc

import kotlin.math.pow

enum class AccidentalType(val symbol: String, val semitoneShift: Int) {
    NONE("", 0),
    SHARP("♯", 1),
    FLAT("♭", -1),
    NATURAL("♮", 0),
    DOUBLE_SHARP("𝄪", 2),
    DOUBLE_FLAT("𝄫", -2)
}

data class AbcNote(
    val letter: Char, // 'C', 'D', 'E', 'F', 'G', 'A', 'B'
    val octave: Int,  // e.g. 4 for C4, 5 for c5
    val accidental: AccidentalType = AccidentalType.NONE,
    val durationBeats: Float = 1.0f, // in quarter-note beats
    val isRest: Boolean = false,
    val measureIndex: Int = 0,
    val noteIndexInScore: Int = 0
) {
    /**
     * MIDI note value based on letter, octave and accidental
     */
    val midiNote: Int
        get() {
            if (isRest) return 0
            val baseSemis = when (letter.uppercaseChar()) {
                'C' -> 0
                'D' -> 2
                'E' -> 4
                'F' -> 5
                'G' -> 7
                'A' -> 9
                'B' -> 11
                else -> 0
            }
            return (octave + 1) * 12 + baseSemis + accidental.semitoneShift
        }

    /**
     * Exact frequency in Hz (A4 = 440 Hz)
     */
    val frequencyHz: Float
        get() {
            if (isRest) return 0f
            return 440f * (2.0.pow((midiNote - 69.0) / 12.0)).toFloat()
        }

    /**
     * Position on treble clef staff:
     * Line 1 (bottom line E4) = -4
     * Line 2 (G4) = -2
     * Line 3 (B4, middle line) = 0
     * Line 4 (D5) = +2
     * Line 5 (F5, top line) = +4
     * Spaces: F4 = -3, A4 = -1, C5 = +1, E5 = +3
     * C4 (ledger line below) = -6
     */
    val staffStep: Int
        get() {
            val stepInOctave = when (letter.uppercaseChar()) {
                'C' -> 0
                'D' -> 1
                'E' -> 2
                'F' -> 3
                'G' -> 4
                'A' -> 5
                'B' -> 6
                else -> 0
            }
            // In octave 4: B4 is step 6 in octave 4. Let B4 be 0:
            // B4: octave=4, step=6 => (4 - 4) * 7 + (6 - 6) = 0
            // C5: octave=5, step=0 => (5 - 4) * 7 + (0 - 6) = +1
            // A4: octave=4, step=5 => (4 - 4) * 7 + (5 - 6) = -1
            return (octave - 4) * 7 + (stepInOctave - 6)
        }

    val displayName: String
        get() {
            if (isRest) return "Silencio"
            return "$letter${accidental.symbol}$octave"
        }
}

data class AbcMeasure(
    val measureNumber: Int,
    val notes: List<AbcNote>,
    val hasRepeatEnd: Boolean = false,
    val hasRepeatStart: Boolean = false
)

data class AbcScore(
    val title: String,
    val composer: String = "Anónimo / Tradicional",
    val meter: String = "4/4",
    val defaultLength: String = "1/4",
    val tempoBpm: Int = 90,
    val key: String = "C",
    val rawAbc: String,
    val measures: List<AbcMeasure>
) {
    val allNotes: List<AbcNote> = measures.flatMap { it.notes }
}
