package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.AcousticState
import com.example.dsp.NotationMode
import com.example.etymology.EtymologyRepository
import com.example.ui.components.CentGaugeView
import com.example.ui.components.ChromaVisualizer
import com.example.ui.components.LiveAcousticMeter
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AcousticAnalyzerScreen(
    state: AcousticState,
    hasRecordPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleMic: () -> Unit,
    onStartCalibration: () -> Unit,
    onResetBuffer: () -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onReferenceA4Change: (Float) -> Unit,
    onNotationModeChange: (NotationMode) -> Unit,
    onNavigateToEtymology: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showA4Dialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .testTag("acoustic_analyzer_screen")
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Permission Banner if not granted
        if (!hasRecordPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCardElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentOrange, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Permiso de micrófono requerido",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tonalia Pro requiere acceso al micrófono para capturar audio en tiempo real y ejecutar el algoritmo DSP YIN.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        modifier = Modifier.testTag("btn_request_permission")
                    ) {
                        Text("Otorgar permiso de audio", color = TextPrimary)
                    }
                }
            }
        }

        // DSP & Audio Control Section
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
                        text = "DSP & MOTOR DE AUDIO",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    // Reference A4 quick badge
                    Surface(
                        color = BgCardElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { showA4Dialog = true }
                            .border(1.dp, BorderHighlight, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "A4 = ${state.referenceA4.toInt()} Hz",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Control Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasRecordPermission) onRequestPermission() else onToggleMic()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isRunning) AccentRed else AccentGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_mic_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isRunning) "Detener" else "Activar Mic",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onStartCalibration,
                        enabled = state.isRunning && !state.isCalibrating,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_calibrate_silence")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (state.isCalibrating) AccentOrange else AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isCalibrating) "${state.calibrationCountdownSec}s..." else "Calibrar",
                            fontSize = 12.sp,
                            color = if (state.isCalibrating) AccentOrange else TextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onResetBuffer,
                        enabled = state.isRunning,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reset_buffer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                // Status message bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgInset, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when {
                                    state.isCalibrating -> AccentOrange
                                    state.isRunning -> AccentGreen
                                    else -> TextTertiary
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.calibrationStatus,
                        color = if (state.isCalibrating) AccentOrange else TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }

                // Telemetry Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgInset, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TelemetryRow(label = "Noise Gate Threshold:", value = "${"%.1f".format(state.noiseGateThresholdDb)} dB", valueColor = AccentOrange)
                    TelemetryRow(label = "Frecuencia Base (A4):", value = "${"%.2f".format(state.referenceA4)} Hz", valueColor = AccentGold)
                    TelemetryRow(label = "Cuadros con Tono:", value = "${state.totalPitchedFrames} frames", valueColor = AccentTeal)
                    TelemetryRow(
                        label = "Señal de Entrada (RMS):",
                        value = if (state.isRunning) "${"%.1f".format(state.currentRmsDb)} dB" else "-∞ dB",
                        valueColor = if (state.normalizedRms > state.normalizedThreshold) AccentGreen else TextSecondary
                    )
                }

                // Live Acoustic Level Meter
                LiveAcousticMeter(
                    normalizedRms = state.normalizedRms,
                    normalizedThreshold = state.normalizedThreshold,
                    rmsDb = state.currentRmsDb,
                    thresholdDb = state.noiseGateThresholdDb,
                    sensitivity = state.sensitivity,
                    onSensitivityChange = onSensitivityChange
                )
            }
        }

        // Acoustic Analyzer Display (Big Note & Cents Gauge)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Notation mode switcher (Letras vs Solfeo)
                Row(
                    modifier = Modifier
                        .background(BgInset, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SegmentButton(
                        text = "Letras (C D E)",
                        isSelected = state.notationMode == NotationMode.LETTERS,
                        onClick = { onNotationModeChange(NotationMode.LETTERS) },
                        testTag = "btn_notation_letters"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SegmentButton(
                        text = "Solfeo (Do Re Mi)",
                        isSelected = state.notationMode == NotationMode.SOLFEGE,
                        onClick = { onNotationModeChange(NotationMode.SOLFEGE) },
                        testTag = "btn_notation_solfege"
                    )
                }

                // Big Note Display
                val pitch = state.currentPitch
                val noteDisplay = if (pitch != null) {
                    if (state.notationMode == NotationMode.LETTERS) pitch.noteLetter else pitch.noteSolfa
                } else "—"

                val octaveDisplay = pitch?.octave?.toString() ?: ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = noteDisplay,
                            color = when {
                                pitch == null -> TextTertiary
                                state.isInTune -> AccentGreen
                                else -> AccentTeal
                            },
                            fontSize = 68.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        )
                        if (octaveDisplay.isNotEmpty()) {
                            Text(
                                text = octaveDisplay,
                                color = TextSecondary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                            )
                        }
                    }

                    // Frequency readout in Hz
                    Text(
                        text = if (pitch != null) {
                            "${"%.2f".format(pitch.frequencyHz)} Hz · MIDI ${pitch.midiNote}"
                        } else {
                            "0.00 Hz"
                        },
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Cents Gauge
                CentGaugeView(
                    cents = state.smoothedCents,
                    isInTune = state.isInTune,
                    hasPitch = pitch != null
                )

                // Quick Etymology preview banner
                if (pitch != null) {
                    val detail = EtymologyRepository.getNoteHistory(pitch.midiNote, state.referenceA4)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgInset, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .clickable { onNavigateToEtymology() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Etimología: ${detail.solfa} (${detail.letter})",
                                color = AccentGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = detail.syllableOrigin,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                        Text(
                            text = "Ver más →",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Tonality & Chroma 12D visualizer card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            ChromaVisualizer(
                chroma = state.chromaVector,
                detectedKeys = state.detectedKeys,
                notationMode = state.notationMode,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // A4 Reference Tuning Dialog
    if (showA4Dialog) {
        var tempA4 by remember { mutableStateOf(state.referenceA4) }
        AlertDialog(
            onDismissRequest = { showA4Dialog = false },
            title = {
                Text("Frecuencia Base A4 (Diapasón)", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Ajustá la frecuencia patrón para calibrar orquestas o afinaciones históricas (415 Hz Barroco, 432 Hz Verdi, 440 Hz ISO estándar, 442 Hz Europa).",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${"%.1f".format(tempA4)} Hz",
                        color = AccentGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = tempA4,
                        onValueChange = { tempA4 = it },
                        valueRange = 415f..446f,
                        steps = 30,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReferenceA4Change(tempA4)
                        showA4Dialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Guardar", color = BgInset, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showA4Dialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = BgCardElevated
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isSelected) AccentTeal else BgInset,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            color = if (isSelected) BgInset else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
