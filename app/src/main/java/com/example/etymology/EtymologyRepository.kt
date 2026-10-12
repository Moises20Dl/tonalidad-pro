package com.example.etymology

import kotlin.math.pow

object EtymologyRepository {

    val HYMN_VERSES = listOf(
        HymnLine("Ut queant laxis", "Ut", "Ut / Do", "Para que con cuerdas vocales desatadas", 0),
        HymnLine("Resonare fibris", "Re", "Re", "puedan hacer resonar", 2),
        HymnLine("Mira gestorum", "Mi", "Mi", "las maravillas de tus hechos", 4),
        HymnLine("Famuli tuorum", "Fa", "Fa", "los siervos tuyos,", 5),
        HymnLine("Solve polluti", "Sol", "Sol", "absuelve y limpia la mancha", 7),
        HymnLine("Labii reatum", "La", "La", "del labio manchado,", 9),
        HymnLine("Sancte Iohannes", "SI", "Si", "¡oh, San Juan Bautista!", 11)
    )

    private val NOTE_LETTERS = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val NOTE_SOLFA = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    private val NATURALS = listOf(0, 2, 4, 5, 7, 9, 11)

    private val SYLLABLE_TEXT = mapOf(
        0 to "Originalmente se cantaba Ut, primera palabra del himno medieval «Ut queant laxis» (en latín ut: «para que»). Hacia 1640, el musicólogo Giovanni Battista Doni propuso cambiarlo por Do para facilitar el solfeo vocal por terminar en vocal abierta sonora. Se postula que proviene de Dominus («Señor») o de la primera sílaba de su propio apellido (Doni).",
        2 to "Re procede de resonare («hacer resonar, vibrar»), el segundo verso del himno a San Juan Bautista cantado un tono por encima de Ut.",
        4 to "Mi proviene de mira («maravillas»), en el tercer verso «Mira gestorum» («las maravillas de tus obras»).",
        5 to "Fa es la sílaba inicial de famuli («los siervos, devotos»), en el cuarto verso «Famuli tuorum».",
        7 to "Sol proviene de solve («absuelve, disuelve, desata»), del verso «Solve polluti».",
        9 to "La es la sílaba de labii («del labio»), correspondiente al verso «Labii reatum». Era la nota más aguda del hexacordo original de Guido d'Arezzo.",
        11 to "Si no formaba parte del hexacordo de Guido (siglo XI). Fue introducida hacia el siglo XVI combinando las iniciales de Sancte Iohannes («San Juan»), cerrando la escala heptatónica. En el solfeo móvil anglosajón se modificó a Ti (Sarah Glover, s. XIX) para evitar confusión fonética con el Sol sostenido."
    )

    private val LETTER_ORDINAL = mapOf(
        9 to "primera (A)",
        11 to "segunda (B)",
        0 to "tercera (C)",
        2 to "cuarta (D)",
        4 to "quinta (E)",
        5 to "sexta (F)",
        7 to "séptima (G)"
    )

    fun getNoteHistory(midi: Int, refA4: Float = 440f): NoteHistoricalDetail {
        val pitchClass = ((midi % 12) + 12) % 12
        val octave = (midi / 12) - 1
        val isSharp = pitchClass !in NATURALS
        val basePitchClass = if (isSharp) pitchClass - 1 else pitchClass

        val letter = NOTE_LETTERS[pitchClass]
        val solfa = NOTE_SOLFA[pitchClass]
        val freq = refA4 * (2.0.pow((midi - 69.0) / 12.0)).toFloat()

        val sylText = SYLLABLE_TEXT[basePitchClass] ?: "Sílaba de origen modal."
        val letterBase = NOTE_LETTERS[basePitchClass]
        val ordinal = LETTER_ORDINAL[basePitchClass] ?: ""

        var letterText = "«$letterBase» es la $ordinal letra del alfabeto latino en la notación alfabética atribuida a Boecio (siglo VI) y monjes carolingios. La serie original arrancaba en la nota A (La), motivo por el cual Do se asignó a la letra C."
        if (basePitchClass == 11) {
            letterText += " En la tradición germánica, B representa Si♭ y H representa Si natural: la «b redonda» y la «b cuadrada» medievales se transformaron gráficamente en dos caracteres distintos."
        }

        val accidentalHistory = if (isSharp) {
            "El sostenido (♯) eleva la entonación medio tono. Su nombre deriva del participio de sostener (latín sustinēre). Nació históricamente de la «b cuadrada» (b quadratum o b durum) de los cantores medievales para indicar el Si natural duro, forma que también dio origen al becuadro (♮). Su contraparte bemol (♭) proviene de la «b molle» (b blanda y redonda)."
        } else null

        val enharmonicName = if (isSharp) {
            val nextPc = (pitchClass + 1) % 12
            "${NOTE_SOLFA[nextPc]}♭ (${NOTE_LETTERS[nextPc]}♭)"
        } else null

        val acousticCuriosity = when (midi) {
            69 -> "La de concierto estándar (A4 = 440 Hz): establecido como patrón internacional por la norma ISO 16 en 1955 para unificar la afinación orquestal mundial."
            60 -> "Do Central (C4 ≈ 261.63 Hz): punto equidistante y bisagra entre la clave de Fa (mano izquierda) y la clave de Sol (mano derecha) en el gran pentagrama del piano."
            else -> null
        }

        val hymnIdx = when (basePitchClass) {
            0 -> 0
            2 -> 1
            4 -> 2
            5 -> 3
            7 -> 4
            9 -> 5
            11 -> 6
            else -> 0
        }

        return NoteHistoricalDetail(
            midiNote = midi,
            pitchClass = pitchClass,
            letter = letter,
            solfa = solfa,
            octave = octave,
            frequencyHz = freq,
            syllableOrigin = sylText,
            letterOrigin = letterText,
            accidentalHistory = accidentalHistory,
            enharmonicName = enharmonicName,
            acousticCuriosity = acousticCuriosity,
            hymnIndex = hymnIdx
        )
    }

    val GLOSSARY = listOf(
        GlossaryTerm(
            term = "Nota",
            category = "Notación",
            etymology = "Del latín nota: «marca, signo o señal escrita».",
            acousticTheory = "Frecuencia fundamental discreta cuantizada en el espacio auditivo continuo.",
            practicalApplication = "Permite la codificación gráfica unívoca de alturas tonales y duraciones en el pentagrama."
        ),
        GlossaryTerm(
            term = "Música",
            category = "Acústica y Arte",
            etymology = "Del griego mousikḗ téchnē: «el arte de las Musas».",
            acousticTheory = "Organización temporal de frecuencias sonoras, relaciones armónicas y amplitudes en el espacio aéreo.",
            practicalApplication = "Composición estructurada que combina melodía, armonía polifónica y ritmo cadencial."
        ),
        GlossaryTerm(
            term = "Tono",
            category = "Intervalos",
            etymology = "Del griego tónos: «tensión, tirantez», por la tensión mecánica de la cuerda que vibra en el monocordio.",
            acousticTheory = "Intervalo de segunda mayor equivalente a 200 cents en temperamento igual (razón de frecuencias 9:8 o 10:9 en entonación justa).",
            practicalApplication = "Paso interválico fundamental en la construcción de los modos mayores, menores y eclesiásticos."
        ),
        GlossaryTerm(
            term = "Semitono",
            category = "Intervalos",
            etymology = "Del latín semi- («medio») y el griego tónos («tensión»).",
            acousticTheory = "Mínimo intervalo de la escala temperada moderna: 100 cents (factor multiplicativo de frecuencia de 2^(1/12) ≈ 1.05946).",
            practicalApplication = "Distancia exacta entre dos trastes contiguos de guitarra o dos teclas adyacentes de piano."
        ),
        GlossaryTerm(
            term = "Octava",
            category = "Intervalos",
            etymology = "Del latín octāva: «la octava parte de un ciclo».",
            acousticTheory = "Relación de frecuencias exacta 2:1. La duplicación matemática de ciclos por segundo genera equivalencia de altura perceptiva.",
            practicalApplication = "Fenómeno de consonancia perfecta donde dos voces cantan notas con idéntico nombre a registros separados."
        ),
        GlossaryTerm(
            term = "Cromático",
            category = "Escalas",
            etymology = "Del griego khrôma: «color».",
            acousticTheory = "Escala de doce alturas equidistantes que «colorea» la escala diatónica natural de siete notas con alteraciones accidentales.",
            practicalApplication = "Modulaciones tonales suaves, pasajes virtuosísticos y acordes de intercambio modal."
        ),
        GlossaryTerm(
            term = "Mayor / menor",
            category = "Modalidad",
            etymology = "Del latín maior («más grande») y minor («más pequeño»).",
            acousticTheory = "Diferenciado por el intervalo de tercera desde la tónica: 4 semitonos (400 cents) en mayor; 3 semitonos (300 cents) en menor.",
            practicalApplication = "Define el carácter psicoacústico y afectivo de una tonalidad (estabilidad brillante vs. introspección melancólica)."
        ),
        GlossaryTerm(
            term = "Hercio (Hz)",
            category = "Física Acústica",
            etymology = "Epónimo en honor al físico alemán Heinrich Hertz (1857–1894).",
            acousticTheory = "Unidad internacional de frecuencia equivalente a un ciclo de compresión y rarefacción sonora por segundo.",
            practicalApplication = "Afinación instrumental exacta: La4 a 440 Hz implica que la columna de aire oscila exactamente 440 veces cada segundo."
        ),
        GlossaryTerm(
            term = "Cent (¢)",
            category = "Microafinación",
            etymology = "Del latín centum: «cien». Introducido por Alexander J. Ellis en 1885.",
            acousticTheory = "Medida logarítmica interválica: c = 1200 * log2(f1 / f0). Hay 100 cents en un semitono y 1200 cents en una octava completa.",
            practicalApplication = "Permite a los afinadores y al analizador Tonalia Pro medir desvíos micrométricos de entonación con precisión quirúrgica."
        ),
        GlossaryTerm(
            term = "Calibrar",
            category = "DSP y Acústica",
            etymology = "Del árabe qālib: «molde, horma de referencia».",
            acousticTheory = "Muestreo y sustracción del espectro de ruido base para fijar una compuerta dinámica (noise gate) adaptativa.",
            practicalApplication = "Elimina zumbidos de aparatos eléctricos y reverberación ambiental para aislar con pureza la voz del instrumentista."
        ),
        GlossaryTerm(
            term = "Enarmonía",
            category = "Teoría Tonal",
            etymology = "Del griego en- («en») y harmonía («proporción, concordancia»).",
            acousticTheory = "Dos nombres distintos para la misma frecuencia física exacta en temperamento igual (p. ej. Re♯ y Mi♭ ambos a 311.13 Hz).",
            practicalApplication = "Permite reinterpretar acordes modulantes para transitar con suavidad a tonalidades lejanas."
        ),
        GlossaryTerm(
            term = "Diapasón",
            category = "Afinación",
            etymology = "Del griego dià pasôn khordôn: «a través de todas las cuerdas vocales».",
            acousticTheory = "Horquilla oscilante de aleación metálica que produce una onda senoidal pura libre de sobretonos espurios.",
            practicalApplication = "Referencia auditiva indiscutible para directores de coro, luthiers y afinadores de instrumentos."
        ),
        GlossaryTerm(
            term = "Krumhansl-Schmuckler",
            category = "Algoritmos DSP",
            etymology = "Nombrado por los psicólogos musicales Carol L. Krumhansl y Mark A. Schmuckler.",
            acousticTheory = "Modelo matemático de correlación estadística de Pearson entre el vector cromático medido y las jerarquías de estabilidad tonal.",
            practicalApplication = "Permite a Tonalia Pro deducir en tiempo real si una melodía o improvisación está en Sol Mayor, Re menor u otra de las 24 tonalidades."
        )
    )
}
