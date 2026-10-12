package com.example.dsp

import kotlin.math.sqrt

data class KeyCandidate(
    val tonicPitchClass: Int,
    val isMinor: Boolean,
    val correlation: Float,
    val keyNameLetters: String,
    val keyNameSolfa: String
)

object TonalityDetector {
    private val NOTE_LETTERS = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val NOTE_SOLFA = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")

    // Krumhansl-Kessler key profiles
    val MAJOR_PROFILE = floatArrayOf(
        6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f
    )
    val MINOR_PROFILE = floatArrayOf(
        6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f
    )

    fun pearsonCorrelation(x: FloatArray, y: FloatArray): Float {
        val n = x.size
        var meanX = 0f
        var meanY = 0f
        for (i in 0 until n) {
            meanX += x[i]
            meanY += y[i]
        }
        meanX /= n
        meanY /= n

        var num = 0f
        var denX = 0f
        var denY = 0f
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        if (denX == 0f || denY == 0f) return 0f
        return (num / sqrt(denX * denY)).coerceIn(-1f, 1f)
    }

    /**
     * Evaluates all 24 musical keys against the given 12-element chroma vector.
     * Returns list of keys sorted descending by correlation.
     */
    fun detectKeys(chroma: FloatArray): List<KeyCandidate> {
        val results = mutableListOf<KeyCandidate>()
        val rotated = FloatArray(12)

        for (tonic in 0 until 12) {
            // Rotate chroma to tonic root
            for (i in 0 until 12) {
                rotated[i] = chroma[(tonic + i) % 12]
            }

            val rMaj = pearsonCorrelation(rotated, MAJOR_PROFILE)
            val rMin = pearsonCorrelation(rotated, MINOR_PROFILE)

            results.add(
                KeyCandidate(
                    tonicPitchClass = tonic,
                    isMinor = false,
                    correlation = rMaj,
                    keyNameLetters = "${NOTE_LETTERS[tonic]} Mayor",
                    keyNameSolfa = "${NOTE_SOLFA[tonic]} Mayor"
                )
            )
            results.add(
                KeyCandidate(
                    tonicPitchClass = tonic,
                    isMinor = true,
                    correlation = rMin,
                    keyNameLetters = "${NOTE_LETTERS[tonic]} menor",
                    keyNameSolfa = "${NOTE_SOLFA[tonic]} menor"
                )
            )
        }

        results.sortByDescending { it.correlation }
        return results
    }
}
