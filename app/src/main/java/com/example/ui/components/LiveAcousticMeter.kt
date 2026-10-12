package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LiveAcousticMeter(
    normalizedRms: Float,
    normalizedThreshold: Float,
    rmsDb: Float,
    thresholdDb: Float,
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("live_acoustic_meter")
            .fillMaxWidth()
            .background(BgInset, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Meter Title & dB Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NIVEL DE ENTRADA (RMS)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = "${"%.1f".format(rmsDb)} dB",
                color = if (normalizedRms > normalizedThreshold) AccentGreen else TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level Bar with Orange Threshold Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Background track
                drawRoundRect(
                    color = BgCardElevated,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )

                // Active level fill
                val fillWidth = (normalizedRms * w).coerceIn(0f, w)
                if (fillWidth > 0f) {
                    val brush = Brush.horizontalGradient(
                        colors = listOf(AccentTeal, AccentGreen, AccentGold),
                        startX = 0f,
                        endX = w
                    )
                    drawRoundRect(
                        brush = brush,
                        size = Size(fillWidth, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f)
                    )
                }

                // Noise Gate Threshold Line (orange vertical bar)
                val thresholdX = (normalizedThreshold * w).coerceIn(2f, w - 2f)
                drawLine(
                    color = AccentOrange,
                    start = Offset(thresholdX, -2.dp.toPx()),
                    end = Offset(thresholdX, h + 2.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Legend below bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Silencio",
                color = TextTertiary,
                fontSize = 10.sp
            )
            Text(
                text = "Umbral Noise Gate: ${"%.1f".format(thresholdDb)} dB",
                color = AccentOrange,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "0 dB",
                color = TextTertiary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sensitivity Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sensibilidad",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Slider(
                value = sensitivity,
                onValueChange = onSensitivityChange,
                valueRange = 1.2f..6.0f,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sensitivity_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = AccentTeal,
                    activeTrackColor = AccentTeal,
                    inactiveTrackColor = BgCardElevated
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${"%.1f".format(sensitivity)}×",
                color = AccentTeal,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
