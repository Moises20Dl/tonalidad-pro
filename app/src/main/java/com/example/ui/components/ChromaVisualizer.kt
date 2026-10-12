package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.KeyCandidate
import com.example.dsp.NotationMode
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ChromaVisualizer(
    chroma: FloatArray,
    detectedKeys: List<KeyCandidate>,
    notationMode: NotationMode,
    modifier: Modifier = Modifier
) {
    val noteLabels = if (notationMode == NotationMode.LETTERS) {
        listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    } else {
        listOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    }

    val maxVal = chroma.maxOrNull()?.coerceAtLeast(0.001f) ?: 1f
    val topKey = detectedKeys.firstOrNull()
    val tonicClass = topKey?.tonicPitchClass ?: -1

    Column(
        modifier = modifier
            .testTag("chroma_visualizer")
            .fillMaxWidth()
            .background(BgInset, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ESPECTRO CROMÁTICO (CHROMA 12D)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )

            if (topKey != null && topKey.correlation > 0.45f) {
                Text(
                    text = "Tonalidad: ${if (notationMode == NotationMode.LETTERS) topKey.keyNameLetters else topKey.keyNameSolfa}",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 12 Chroma Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until 12) {
                val ratio = (chroma.getOrElse(i) { 0f } / maxVal).coerceIn(0.04f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    label = "chroma_bar_$i"
                )
                val isTonic = i == tonicClass && topKey != null && topKey.correlation > 0.45f
                val isDominant = maxVal > 0.1f && chroma.getOrElse(i) { 0f } >= maxVal * 0.85f

                val barColor = when {
                    isTonic -> AccentGreen
                    isDominant -> AccentTeal
                    else -> AccentPurple.copy(alpha = 0.75f)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedRatio)
                            .background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Note Labels underneath bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until 12) {
                val isTonic = i == tonicClass && topKey != null && topKey.correlation > 0.45f
                Text(
                    text = noteLabels[i],
                    modifier = Modifier.weight(1f),
                    color = if (isTonic) AccentGreen else TextTertiary,
                    fontSize = if (notationMode == NotationMode.LETTERS) 10.sp else 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isTonic) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tonality Detection Result Card (Krumhansl-Schmuckler)
        if (topKey != null && topKey.correlation > 0.35f) {
            val keyDisplay = if (notationMode == NotationMode.LETTERS) topKey.keyNameLetters else topKey.keyNameSolfa
            val confPct = (topKey.correlation * 100f).coerceIn(0f, 100f).toInt()
            val confLabel = when {
                topKey.correlation > 0.75f -> "Alta"
                topKey.correlation > 0.55f -> "Media"
                else -> "Preliminar"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCardElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = keyDisplay,
                        color = AccentGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Confianza: $confPct% ($confLabel)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Confidence bar
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(BgInset, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(topKey.correlation.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AccentTeal, RoundedCornerShape(2.dp))
                    )
                }

                // Secondary candidates
                if (detectedKeys.size >= 3) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val cand2 = detectedKeys[1]
                    val cand3 = detectedKeys[2]
                    val cand2Name = if (notationMode == NotationMode.LETTERS) cand2.keyNameLetters else cand2.keyNameSolfa
                    val cand3Name = if (notationMode == NotationMode.LETTERS) cand3.keyNameLetters else cand3.keyNameSolfa
                    Text(
                        text = "Alternativas: $cand2Name (${(cand2.correlation * 100).toInt()}%) · $cand3Name (${(cand3.correlation * 100).toInt()}%)",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            Text(
                text = "Tocá o cantá varias notas durante unos segundos para calcular la tonalidad armónica.",
                color = TextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
