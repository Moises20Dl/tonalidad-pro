package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.BorderDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.abs

@Composable
fun CentGaugeView(
    cents: Float,
    isInTune: Boolean,
    hasPitch: Boolean,
    modifier: Modifier = Modifier
) {
    val clampedCents = if (hasPitch) cents.coerceIn(-50f, 50f) else 0f
    val animatedCents by animateFloatAsState(
        targetValue = clampedCents,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.75f),
        label = "cents_needle"
    )

    val needleColor = when {
        !hasPitch -> TextTertiary
        isInTune -> AccentGreen
        animatedCents > 15f -> AccentGold
        animatedCents > 30f -> AccentRed
        animatedCents < -15f -> AccentTeal
        else -> TextPrimary
    }

    Column(
        modifier = modifier
            .testTag("cent_gauge_view")
            .fillMaxWidth()
            .background(BgInset, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Needle Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val midY = h * 0.45f

                // Background track
                drawRoundRect(
                    color = BgCardElevated,
                    topLeft = Offset(0f, midY - 3.dp.toPx()),
                    size = Size(w, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // In-tune center target zone (-5 to +5 cents)
                val zoneHalfW = (w / 100f) * 6f
                drawRoundRect(
                    color = AccentGreen.copy(alpha = if (isInTune) 0.35f else 0.15f),
                    topLeft = Offset((w / 2f) - zoneHalfW, midY - 6.dp.toPx()),
                    size = Size(zoneHalfW * 2f, 12.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Graduation ticks
                for (c in -50..50 step 10) {
                    val tickX = (w / 2f) + (c / 50f) * (w / 2f)
                    val isCenter = c == 0
                    val isMajor = c % 25 == 0

                    val tickH = when {
                        isCenter -> 20.dp.toPx()
                        isMajor -> 14.dp.toPx()
                        else -> 8.dp.toPx()
                    }
                    val tickColor = when {
                        isCenter -> if (isInTune) AccentGreen else AccentTeal
                        isMajor -> TextSecondary
                        else -> BorderDark
                    }
                    val strokeW = if (isCenter) 2.5.dp.toPx() else 1.2.dp.toPx()

                    drawLine(
                        color = tickColor,
                        start = Offset(tickX, midY - tickH / 2f),
                        end = Offset(tickX, midY + tickH / 2f),
                        strokeWidth = strokeW
                    )
                }

                // Needle indicator
                if (hasPitch) {
                    val needleX = (w / 2f) + (animatedCents / 50f) * (w / 2f)
                    drawRoundRect(
                        color = needleColor,
                        topLeft = Offset(needleX - 3.dp.toPx(), midY - 16.dp.toPx()),
                        size = Size(6.dp.toPx(), 32.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Top pointer triangle / pip
                    drawCircle(
                        color = needleColor,
                        radius = 4.dp.toPx(),
                        center = Offset(needleX, midY - 17.dp.toPx())
                    )
                }
            }
        }

        // Numerical Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♭ -50¢",
                color = AccentTeal,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            if (hasPitch) {
                val sign = if (cents >= 0) "+" else ""
                val statusText = if (isInTune) "AFINADO (0¢)" else "$sign${cents.toInt()}¢"
                val statusColor = if (isInTune) AccentGreen else TextPrimary
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "EN REPOSO",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "+50¢ ♯",
                color = AccentGold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
