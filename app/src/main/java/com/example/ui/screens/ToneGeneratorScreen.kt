package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.dsp.AudioToneGenerator
import com.example.dsp.YinPitchTracker
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
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
import kotlin.math.pow

@Composable
fun ToneGeneratorScreen(
    referenceA4: Float,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var selectedPitchClass by remember { mutableIntStateOf(9) } // A
    var selectedOctave by remember { mutableIntStateOf(4) } // 4
    var centsOffset by remember { mutableFloatStateOf(0f) }
    var volume by remember { mutableFloatStateOf(0.7f) }
    var isPlaying by remember { mutableStateOf(false) }

    val noteLetters = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    val noteSolfa = listOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")

    // Calculate current frequency
    val baseMidi = (selectedOctave + 1) * 12 + selectedPitchClass
    val exactMidi = baseMidi + (centsOffset / 100f)
    val calculatedFreq = referenceA4 * (2.0.pow((exactMidi - 69.0) / 12.0)).toFloat()

    fun updateTone() {
        if (isPlaying) {
            AudioToneGenerator.startContinuousTone(calculatedFreq, volume)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioToneGenerator.stopContinuousTone()
        }
    }

    Column(
        modifier = modifier
            .testTag("tone_generator_screen")
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GENERADOR ACÚSTICO DE TONO",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Diapasón y calibrador de tono puro",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(24.dp)
            )
        }

        // Main Display & Play Button
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${noteLetters[selectedPitchClass]}$selectedOctave · ${noteSolfa[selectedPitchClass]}$selectedOctave",
                    color = if (isPlaying) AccentGreen else AccentGold,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "${"%.2f".format(calculatedFreq)} Hz",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                if (centsOffset != 0f) {
                    val sign = if (centsOffset > 0) "+" else ""
                    Text(
                        text = "Microdesvío: $sign${centsOffset.toInt()} cents",
                        color = AccentTeal,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (isPlaying) {
                            AudioToneGenerator.stopContinuousTone()
                            isPlaying = false
                        } else {
                            AudioToneGenerator.startContinuousTone(calculatedFreq, volume)
                            isPlaying = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) AccentRed else AccentGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(50.dp)
                        .testTag("btn_toggle_tone_playback")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "Silenciar Tono" else "Emitir Tono",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Chromatic Key Selector (12 Notes Grid)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SELECCIÓN DE NOTA FUNDAMENTAL",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4x3 Grid of 12 chromatic pitches
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val isSelected = selectedPitchClass == index
                                Surface(
                                    color = if (isSelected) AccentGold else BgInset,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .border(
                                            1.dp,
                                            if (isSelected) AccentGold else BorderDark,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPitchClass = index
                                            updateTone()
                                        }
                                        .testTag("btn_tone_pitch_$index")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = noteLetters[index],
                                            color = if (isSelected) BgInset else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${noteSolfa[index]})",
                                            color = if (isSelected) BgInset.copy(alpha = 0.8f) else TextTertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Octave Selector (2 to 6)
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "REGISTRO / OCTAVA",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (oct in 2..6) {
                        val isSelected = selectedOctave == oct
                        val label = when (oct) {
                            2 -> "2 (Bajo)"
                            3 -> "3 (Tenor)"
                            4 -> "4 (Central)"
                            5 -> "5 (Agudo)"
                            6 -> "6 (Sobreagudo)"
                            else -> "$oct"
                        }

                        Surface(
                            color = if (isSelected) AccentTeal else BgInset,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentTeal else BorderDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedOctave = oct
                                    updateTone()
                                }
                                .testTag("btn_tone_octave_$oct")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$oct",
                                    color = if (isSelected) BgInset else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Microtonal Cent Adjustment & Volume
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Microafinación (Cents)", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "${if (centsOffset > 0) "+" else ""}${centsOffset.toInt()} ¢",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = centsOffset,
                    onValueChange = {
                        centsOffset = it
                        updateTone()
                    },
                    valueRange = -50f..50f,
                    modifier = Modifier.testTag("cents_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGold,
                        activeTrackColor = AccentGold
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Volumen de salida", color = TextSecondary, fontSize = 11.sp)
                    }

                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = AccentTeal,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        updateTone()
                    },
                    valueRange = 0.05f..1f,
                    modifier = Modifier.testTag("volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentTeal,
                        activeTrackColor = AccentTeal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
