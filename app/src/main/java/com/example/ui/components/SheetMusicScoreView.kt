package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.abc.AbcNote
import com.example.abc.AbcScore
import com.example.abc.AccidentalType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgInset
import kotlin.math.max

@Composable
fun SheetMusicScoreView(
    score: AbcScore,
    currentPlayingNoteIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val totalMeasures = max(1, score.measures.size)
    // Measure width around 220 dp
    val canvasWidthDp = max(380, totalMeasures * 210 + 100).dp

    Box(
        modifier = modifier
            .testTag("sheet_music_score_view")
            .fillMaxWidth()
            .height(230.dp)
            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidthDp)
                .fillMaxHeight()
        ) {
            val staffLineSpacing = 12.dp.toPx()
            val staffTopY = 70.dp.toPx()
            val staffBottomY = staffTopY + 4 * staffLineSpacing
            val staffMiddleY = (staffTopY + staffBottomY) / 2f // B4 line

            // 1. Draw 5 horizontal staff lines
            val staffStartX = 24.dp.toPx()
            val staffEndX = size.width - 24.dp.toPx()

            for (i in 0..4) {
                val y = staffTopY + i * staffLineSpacing
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(staffStartX, y),
                    end = Offset(staffEndX, y),
                    strokeWidth = 1.5f
                )
            }

            // 2. Initial system barline
            drawLine(
                color = Color(0xFF222222),
                start = Offset(staffStartX, staffTopY),
                end = Offset(staffStartX, staffBottomY),
                strokeWidth = 2.5f
            )

            // 3. Treble Clef Symbol (𝄞)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 48.dp.toPx()
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.SERIF
                }
                drawText("𝄞", staffStartX + 4.dp.toPx(), staffBottomY - 4.dp.toPx(), paint)
            }

            // 4. Time Signature (e.g. 4/4)
            val timeSigX = staffStartX + 52.dp.toPx()
            val meterParts = score.meter.split("/")
            val num = meterParts.getOrNull(0) ?: "4"
            val den = meterParts.getOrNull(1) ?: "4"

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 22.dp.toPx()
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText(num, timeSigX, staffTopY + 1.8f * staffLineSpacing, paint)
                drawText(den, timeSigX, staffTopY + 3.8f * staffLineSpacing, paint)
            }

            // 5. Draw Measures & Notes
            var currentX = timeSigX + 36.dp.toPx()
            val noteHeadRadiusX = 7.dp.toPx()
            val noteHeadRadiusY = 5.dp.toPx()
            val stemHeight = 34.dp.toPx()

            for (measure in score.measures) {
                val measureStartX = currentX
                val notesInMeasure = measure.notes
                val noteSpacing = if (notesInMeasure.size > 1) {
                    170.dp.toPx() / notesInMeasure.size
                } else {
                    140.dp.toPx()
                }

                // Draw measure number
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                    }
                    drawText("${measure.measureNumber}", measureStartX + 4.dp.toPx(), staffTopY - 14.dp.toPx(), paint)
                }

                for (note in notesInMeasure) {
                    val noteX = currentX + 24.dp.toPx()
                    val isPlaying = currentPlayingNoteIndex == note.noteIndexInScore

                    if (note.isRest) {
                        // Draw rest symbol
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (isPlaying) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.BLACK
                                textSize = 22.dp.toPx()
                                isAntiAlias = true
                            }
                            drawText("𝄽", noteX - 6.dp.toPx(), staffMiddleY + 8.dp.toPx(), paint)
                        }
                    } else {
                        // Note Y calculation: staffMiddleY is B4 (step 0).
                        // Each step is half the staff line spacing (6.dp).
                        // Higher pitch = lower Y (step up).
                        val stepY = staffMiddleY - (note.staffStep * (staffLineSpacing / 2f))

                        // Highlight box if playing
                        if (isPlaying) {
                            drawRoundRect(
                                color = AccentGreen.copy(alpha = 0.25f),
                                topLeft = Offset(noteX - 18.dp.toPx(), staffTopY - 24.dp.toPx()),
                                size = Size(36.dp.toPx(), (staffBottomY - staffTopY) + 48.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        // Ledger lines
                        // Middle C (C4) is step -6 (line below staff)
                        if (note.staffStep <= -6) {
                            val ledgerY = staffMiddleY + 6 * (staffLineSpacing / 2f)
                            drawLine(
                                color = Color(0xFF444444),
                                start = Offset(noteX - 12.dp.toPx(), ledgerY),
                                end = Offset(noteX + 12.dp.toPx(), ledgerY),
                                strokeWidth = 1.5f
                            )
                        } else if (note.staffStep >= 6) {
                            val ledgerY = staffMiddleY - 6 * (staffLineSpacing / 2f)
                            drawLine(
                                color = Color(0xFF444444),
                                start = Offset(noteX - 12.dp.toPx(), ledgerY),
                                end = Offset(noteX + 12.dp.toPx(), ledgerY),
                                strokeWidth = 1.5f
                            )
                        }

                        // Accidental (♭, ♯, ♮)
                        if (note.accidental != AccidentalType.NONE) {
                            val accSymbol = when (note.accidental) {
                                AccidentalType.FLAT -> "♭"
                                AccidentalType.SHARP -> "♯"
                                AccidentalType.NATURAL -> "♮"
                                AccidentalType.DOUBLE_FLAT -> "𝄫"
                                AccidentalType.DOUBLE_SHARP -> "𝄪"
                                else -> ""
                            }
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = if (isPlaying) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.BLACK
                                    textSize = 20.dp.toPx()
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                                drawText(accSymbol, noteX - 18.dp.toPx(), stepY + 6.dp.toPx(), paint)
                            }
                        }

                        // Notehead (oval tilted slightly)
                        val noteColor = if (isPlaying) AccentGreen else Color(0xFF1E1E1E)
                        val isFilled = note.durationBeats < 2.0f

                        drawOval(
                            color = noteColor,
                            topLeft = Offset(noteX - noteHeadRadiusX, stepY - noteHeadRadiusY),
                            size = Size(noteHeadRadiusX * 2, noteHeadRadiusY * 2),
                            style = if (isFilled) Fill else Stroke(width = 2.5f)
                        )

                        // Note Stem
                        // If pitch >= B4 (step 0), stem points DOWN on left side;
                        // if pitch < B4, stem points UP on right side.
                        if (note.durationBeats < 4.0f) { // whole notes have no stem
                            val stemUp = note.staffStep < 0
                            val stemX = if (stemUp) noteX + noteHeadRadiusX - 1f else noteX - noteHeadRadiusX + 1f
                            val stemEndY = if (stemUp) stepY - stemHeight else stepY + stemHeight

                            drawLine(
                                color = noteColor,
                                start = Offset(stemX, stepY),
                                end = Offset(stemX, stemEndY),
                                strokeWidth = 2f
                            )

                            // Eighth note flag
                            if (note.durationBeats <= 0.5f) {
                                val flagPath = Path().apply {
                                    moveTo(stemX, stemEndY)
                                    if (stemUp) {
                                        quadraticTo(
                                            stemX + 12.dp.toPx(),
                                            stemEndY + 8.dp.toPx(),
                                            stemX + 8.dp.toPx(),
                                            stemEndY + 18.dp.toPx()
                                        )
                                    } else {
                                        quadraticTo(
                                            stemX + 12.dp.toPx(),
                                            stemEndY - 8.dp.toPx(),
                                            stemX + 8.dp.toPx(),
                                            stemEndY - 18.dp.toPx()
                                        )
                                    }
                                }
                                drawPath(flagPath, noteColor, style = Stroke(width = 2f))
                            }
                        }

                        // Note letter label under staff
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (isPlaying) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.DKGRAY
                                textSize = 11.dp.toPx()
                                isFakeBoldText = isPlaying
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(note.displayName, noteX, staffBottomY + 22.dp.toPx(), paint)
                        }
                    }

                    currentX += noteSpacing
                }

                // Draw measure barline
                if (measure.hasRepeatEnd) {
                    // Double repeat barline :|
                    drawLine(
                        color = Color(0xFF222222),
                        start = Offset(currentX + 8.dp.toPx(), staffTopY),
                        end = Offset(currentX + 8.dp.toPx(), staffBottomY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF222222),
                        start = Offset(currentX + 14.dp.toPx(), staffTopY),
                        end = Offset(currentX + 14.dp.toPx(), staffBottomY),
                        strokeWidth = 3.5f
                    )
                    // Dots
                    drawCircle(Color(0xFF222222), radius = 2.5.dp.toPx(), center = Offset(currentX + 3.dp.toPx(), staffMiddleY - staffLineSpacing / 2f))
                    drawCircle(Color(0xFF222222), radius = 2.5.dp.toPx(), center = Offset(currentX + 3.dp.toPx(), staffMiddleY + staffLineSpacing / 2f))
                    currentX += 26.dp.toPx()
                } else {
                    drawLine(
                        color = Color(0xFF444444),
                        start = Offset(currentX + 8.dp.toPx(), staffTopY),
                        end = Offset(currentX + 8.dp.toPx(), staffBottomY),
                        strokeWidth = 1.5f
                    )
                    currentX += 16.dp.toPx()
                }
            }
        }
    }
}
