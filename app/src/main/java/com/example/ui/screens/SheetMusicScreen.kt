package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abc.AbcNote
import com.example.abc.AbcParser
import com.example.abc.AbcScore
import com.example.dsp.AudioToneGenerator
import com.example.ui.components.SheetMusicScoreView
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SheetMusicScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedSampleIndex by remember { mutableIntStateOf(0) }
    var abcCode by remember {
        mutableStateOf(AbcParser.SAMPLE_SCORES[0].rawAbc)
    }

    var parsedScore by remember {
        mutableStateOf(AbcParser.parse(abcCode))
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingNoteIndex by remember { mutableStateOf<Int?>(null) }
    var tempoBpm by remember { mutableIntStateOf(parsedScore.tempoBpm) }
    var isLooping by remember { mutableStateOf(false) }
    var isEditorVisible by remember { mutableStateOf(true) }

    var playbackJob by remember { mutableStateOf<Job?>(null) }

    // Re-parse when abcCode changes
    fun updateScore(newCode: String) {
        abcCode = newCode
        try {
            val score = AbcParser.parse(newCode)
            parsedScore = score
            if (tempoBpm == 90 && score.tempoBpm != 90) {
                tempoBpm = score.tempoBpm
            }
        } catch (_: Exception) {}
    }

    // Stop playback
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaying = false
        currentPlayingNoteIndex = null
    }

    // Playback loop
    fun startPlayback() {
        stopPlayback()
        isPlaying = true

        playbackJob = coroutineScope.launch {
            val notes = parsedScore.allNotes
            if (notes.isEmpty()) {
                isPlaying = false
                return@launch
            }

            do {
                for (note in notes) {
                    if (!isActive) break
                    currentPlayingNoteIndex = note.noteIndexInScore

                    val beatDurationMs = (60_000f / tempoBpm).toLong()
                    val noteDurationMs = (beatDurationMs * note.durationBeats).toLong()

                    if (!note.isRest && note.frequencyHz > 0f) {
                        launch {
                            AudioToneGenerator.playNote(
                                frequencyHz = note.frequencyHz,
                                durationMs = (noteDurationMs * 0.9f).toLong(),
                                volume = 0.8f
                            )
                        }
                    }

                    delay(noteDurationMs)
                }
            } while (isLooping && isActive)

            currentPlayingNoteIndex = null
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackJob?.cancel()
        }
    }

    Column(
        modifier = modifier
            .testTag("sheet_music_screen")
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LECTOR DE PARTITURAS ESTRUCTURADO",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = parsedScore.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { isEditorVisible = !isEditorVisible },
                modifier = Modifier.testTag("btn_toggle_abc_editor")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Editar código ABC",
                    tint = if (isEditorVisible) AccentTeal else TextSecondary
                )
            }
        }

        // Library of Educational Scores
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AbcParser.SAMPLE_SCORES.indices.toList()) { index ->
                val item = AbcParser.SAMPLE_SCORES[index]
                val isSelected = selectedSampleIndex == index
                Surface(
                    color = if (isSelected) AccentTeal.copy(alpha = 0.2f) else BgCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) AccentTeal else BorderDark,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            stopPlayback()
                            selectedSampleIndex = index
                            updateScore(item.rawAbc)
                            tempoBpm = item.tempoBpm
                        }
                        .testTag("score_sample_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isSelected) AccentTeal else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.title,
                            color = if (isSelected) AccentTeal else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sheet Music Notation Output View (Compose Canvas)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTACIÓN MUSICAL PROFESIONAL",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tonalidad: ${parsedScore.key} · Compás: ${parsedScore.meter}",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SheetMusicScoreView(
                    score = parsedScore,
                    currentPlayingNoteIndex = currentPlayingNoteIndex
                )
            }
        }

        // Synthesizer & Playback Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPRODUCTOR SINTETIZADOR",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$tempoBpm BPM",
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isPlaying) stopPlayback() else startPlayback()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) AccentGold else AccentGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_play_sheet_music")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pausar" else "Reproducir",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { stopPlayback() },
                        enabled = isPlaying,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_stop_sheet_music")
                    ) {
                        Text("Detener", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { isLooping = !isLooping },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isLooping) AccentTeal else TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_loop_sheet_music")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = null,
                            tint = if (isLooping) AccentTeal else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bucle", fontSize = 12.sp)
                    }
                }

                // Tempo Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Tempo / Velocidad", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "Adagio (60) — Allegro (120)", color = TextTertiary, fontSize = 10.sp)
                    }
                    Slider(
                        value = tempoBpm.toFloat(),
                        onValueChange = { tempoBpm = it.toInt() },
                        valueRange = 40f..200f,
                        modifier = Modifier.testTag("tempo_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal
                        )
                    )
                }
            }
        }

        // Structured ABC Code Editor
        AnimatedVisibility(visible = isEditorVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EDITOR DE CÓDIGO ABC",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Estándar ABC 2.2",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Monospace Code Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(BgInset, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = abcCode,
                            onValueChange = { updateScore(it) },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(AccentTeal),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("abc_code_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sintaxis: C D E c | ^F sostenido | _E bemol | c2 duración doble | :| repetición.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Acoustic & Musical Analysis Note Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCardElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ANÁLISIS METODOLÓGICO Y ACÚSTICO",
                    color = AccentGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val analysisText = when (selectedSampleIndex) {
                    0 -> "La pieza «Análisis de Terceras y Modulación» contrasta el arpegio mayor puro (C - E - G - c) con la modulación de tercera menor cromática (_E - G - B - _e), demostrando la transición psicoacústica de consonancia diatónica a tensión enarmónica."
                    1 -> "El himno «Ut queant laxis» representa el hito más influyente de la notación musical occidental: cada hemistiquio asciende exactamente un tono o semitono sobre el hexacordo natural de Guido d'Arezzo."
                    2 -> "«Oda a la Alegría» utiliza una tesitura vocal contenida (ámbito de quinta justa) con movimiento predominantemente por grados conjuntos, ideal para calibración coral e intonación de quintas."
                    else -> "Las escalas y arpegios diatónicos permiten comprobar la afinación justa y el temperamento igual sobre los armónicos 2.º a 5.º de la fundamental."
                }
                Text(
                    text = analysisText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
