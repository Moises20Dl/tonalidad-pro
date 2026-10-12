package com.example.abc

object AbcParser {

    val SAMPLE_SCORES = listOf(
        AbcScore(
            title = "Análisis de Terceras y Modulación",
            composer = "Suite Tonalia Pro",
            meter = "4/4",
            defaultLength = "1/4",
            tempoBpm = 90,
            key = "C",
            rawAbc = """
                X:1
                T:Análisis de Terceras y Modulación
                M:4/4
                L:1/4
                Q:1/4=90
                K:C
                C E G c | _E G B _e | F A c f | _A c _e _a :|
            """.trimIndent(),
            measures = emptyList() // will be parsed on load
        ),
        AbcScore(
            title = "Himno Ut queant laxis",
            composer = "Guido d'Arezzo (Siglo XI)",
            meter = "4/4",
            defaultLength = "1/4",
            tempoBpm = 80,
            key = "C",
            rawAbc = """
                X:2
                T:Himno Ut queant laxis
                M:4/4
                L:1/4
                Q:1/4=80
                K:C
                C D E C | D E F D | E F G E | F G A F | G A B G | c2 c2 :|
            """.trimIndent(),
            measures = emptyList()
        ),
        AbcScore(
            title = "Oda a la Alegría",
            composer = "L. van Beethoven",
            meter = "4/4",
            defaultLength = "1/4",
            tempoBpm = 110,
            key = "C",
            rawAbc = """
                X:3
                T:Oda a la Alegría
                M:4/4
                L:1/4
                Q:1/4=110
                K:C
                E E F G | G F E D | C C D E | E3/2 D/2 D2 | E E F G | G F E D | C C D E | D3/2 C/2 C2 :|
            """.trimIndent(),
            measures = emptyList()
        ),
        AbcScore(
            title = "Escala Mayor y Arpegios",
            composer = "Ejercicio Pedagógico",
            meter = "4/4",
            defaultLength = "1/4",
            tempoBpm = 95,
            key = "C",
            rawAbc = """
                X:4
                T:Escala Mayor y Arpegios
                M:4/4
                L:1/4
                Q:1/4=95
                K:C
                C D E F | G A B c | c B A G | F E D C | C E G c | G E C2 :|
            """.trimIndent(),
            measures = emptyList()
        )
    )

    fun parse(abcText: String): AbcScore {
        var title = "Partitura Sin Título"
        var composer = "Tradicional"
        var meter = "4/4"
        var defaultLength = "1/4"
        var tempoBpm = 90
        var key = "C"

        val musicBody = StringBuilder()

        val lines = abcText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("%") || trimmed.isEmpty()) continue

            if (trimmed.length >= 2 && trimmed[1] == ':') {
                val tag = trimmed[0].uppercaseChar()
                val value = trimmed.substring(2).trim()
                when (tag) {
                    'T' -> title = value
                    'C' -> composer = value
                    'M' -> meter = value
                    'L' -> defaultLength = value
                    'Q' -> {
                        // e.g. "1/4=90" or "90"
                        val bpmMatch = Regex("(\\d+)").findAll(value).lastOrNull()?.value
                        if (bpmMatch != null) tempoBpm = bpmMatch.toIntOrNull() ?: 90
                    }
                    'K' -> key = value
                }
            } else {
                musicBody.append(trimmed).append(" ")
            }
        }

        val measures = parseMusicBody(musicBody.toString())
        return AbcScore(
            title = title,
            composer = composer,
            meter = meter,
            defaultLength = defaultLength,
            tempoBpm = tempoBpm,
            key = key,
            rawAbc = abcText,
            measures = measures
        )
    }

    private fun parseMusicBody(body: String): List<AbcMeasure> {
        val measures = mutableListOf<AbcMeasure>()
        var currentMeasureNotes = mutableListOf<AbcNote>()
        var globalNoteIndex = 0
        var currentMeasureIndex = 0

        var i = 0
        val len = body.length

        while (i < len) {
            val ch = body[i]

            // Bar line or repeat
            if (ch == '|' || ch == ':') {
                var hasRepeatEnd = false
                var hasRepeatStart = false

                if (ch == ':' && i + 1 < len && body[i + 1] == '|') {
                    hasRepeatEnd = true
                    i++
                } else if (ch == '|' && i + 1 < len && body[i + 1] == ':') {
                    hasRepeatStart = true
                    i++
                } else if (ch == '|' && i + 1 < len && body[i + 1] == ']') {
                    hasRepeatEnd = true
                    i++
                }

                if (currentMeasureNotes.isNotEmpty() || hasRepeatEnd || hasRepeatStart) {
                    measures.add(
                        AbcMeasure(
                            measureNumber = currentMeasureIndex + 1,
                            notes = currentMeasureNotes.toList(),
                            hasRepeatEnd = hasRepeatEnd,
                            hasRepeatStart = hasRepeatStart
                        )
                    )
                    currentMeasureIndex++
                    currentMeasureNotes = mutableListOf()
                }
                i++
                continue
            }

            // Whitespace
            if (ch.isWhitespace()) {
                i++
                continue
            }

            // Accidentals
            var accidental = AccidentalType.NONE
            if (ch == '^') {
                accidental = if (i + 1 < len && body[i + 1] == '^') {
                    i++
                    AccidentalType.DOUBLE_SHARP
                } else {
                    AccidentalType.SHARP
                }
                i++
            } else if (ch == '_') {
                accidental = if (i + 1 < len && body[i + 1] == '_') {
                    i++
                    AccidentalType.DOUBLE_FLAT
                } else {
                    AccidentalType.FLAT
                }
                i++
            } else if (ch == '=') {
                accidental = AccidentalType.NATURAL
                i++
            }

            if (i >= len) break
            val noteChar = body[i]

            // Check if rest or note
            if (noteChar == 'z' || noteChar == 'Z') {
                i++
                val duration = parseDuration(body, i)
                i = duration.second
                currentMeasureNotes.add(
                    AbcNote(
                        letter = 'C',
                        octave = 4,
                        durationBeats = duration.first,
                        isRest = true,
                        measureIndex = currentMeasureIndex,
                        noteIndexInScore = globalNoteIndex++
                    )
                )
                continue
            }

            if (noteChar in "ABCDEFGabcdefg") {
                val isUpper = noteChar.isUpperCase()
                var octave = if (isUpper) 4 else 5
                i++

                // Octave modifiers: ',' lowers, ''' raises
                while (i < len && (body[i] == ',' || body[i] == '\'')) {
                    if (body[i] == ',') octave--
                    else if (body[i] == '\'') octave++
                    i++
                }

                // Duration modifier
                val duration = parseDuration(body, i)
                i = duration.second

                currentMeasureNotes.add(
                    AbcNote(
                        letter = noteChar.uppercaseChar(),
                        octave = octave,
                        accidental = accidental,
                        durationBeats = duration.first,
                        isRest = false,
                        measureIndex = currentMeasureIndex,
                        noteIndexInScore = globalNoteIndex++
                    )
                )
                continue
            }

            // Skip unknown symbols
            i++
        }

        if (currentMeasureNotes.isNotEmpty()) {
            measures.add(
                AbcMeasure(
                    measureNumber = currentMeasureIndex + 1,
                    notes = currentMeasureNotes.toList()
                )
            )
        }

        return measures
    }

    private fun parseDuration(body: String, startIndex: Int): Pair<Float, Int> {
        var idx = startIndex
        val len = body.length
        if (idx >= len) return Pair(1.0f, idx)

        val sb = StringBuilder()
        while (idx < len && (body[idx].isDigit() || body[idx] == '/')) {
            sb.append(body[idx])
            idx++
        }

        val durStr = sb.toString()
        if (durStr.isEmpty()) return Pair(1.0f, idx)

        return try {
            if (durStr == "/") {
                Pair(0.5f, idx)
            } else if (durStr.contains("/")) {
                val parts = durStr.split("/")
                val num = if (parts[0].isEmpty()) 1f else parts[0].toFloat()
                val den = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toFloat() else 2f
                Pair(num / den, idx)
            } else {
                Pair(durStr.toFloat(), idx)
            }
        } catch (_: Exception) {
            Pair(1.0f, idx)
        }
    }
}
