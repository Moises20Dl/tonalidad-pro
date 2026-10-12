package com.example.etymology

data class HymnLine(
    val verseText: String,
    val highlightedWord: String,
    val syllable: String,
    val translation: String,
    val notePitchClass: Int // 0 for Ut/Do, 2 for Re, 4 for Mi, 5 for Fa, 7 for Sol, 9 for La, 11 for Si
)

data class NoteHistoricalDetail(
    val midiNote: Int,
    val pitchClass: Int,
    val letter: String,
    val solfa: String,
    val octave: Int,
    val frequencyHz: Float,
    val syllableOrigin: String,
    val letterOrigin: String,
    val accidentalHistory: String?,
    val enharmonicName: String?,
    val acousticCuriosity: String?,
    val hymnIndex: Int
)

data class GlossaryTerm(
    val term: String,
    val category: String,
    val etymology: String,
    val acousticTheory: String,
    val practicalApplication: String
)
