package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.etymology.EtymologyRepository
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgInset
import com.example.ui.theme.BorderDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LatinHymnCard(
    activeHymnIndex: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("latin_hymn_card")
            .fillMaxWidth()
            .background(BgInset, RoundedCornerShape(12.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HIMNO HISTÓRICO · UT QUEANT LAXIS",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Guido d'Arezzo (s. XI)",
                color = TextTertiary,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Verses with illumination
        EtymologyRepository.HYMN_VERSES.forEachIndexed { index, verse ->
            val isActive = activeHymnIndex == index
            val animatedBg by animateColorAsState(
                targetValue = if (isActive) AccentGreen.copy(alpha = 0.18f) else BgInset,
                label = "hymn_verse_bg_$index"
            )
            val animatedBorder by animateColorAsState(
                targetValue = if (isActive) AccentGreen else BorderDark.copy(alpha = 0.4f),
                label = "hymn_verse_border_$index"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(animatedBg, RoundedCornerShape(6.dp))
                    .border(if (isActive) 1.5.dp else 0.5.dp, animatedBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${verse.syllable}: ",
                        color = if (isActive) AccentGold else AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = verse.verseText,
                        color = if (isActive) TextPrimary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.Serif
                    )
                }

                Text(
                    text = verse.translation,
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Dedicado a San Juan Bautista, atribuido a Pablo el Diácono (s. VIII). Hacia el año 1025, el monje benedictino Guido d'Arezzo observó que cada verso iniciaba un grado más agudo que el anterior y lo utilizó para enseñar la afinación exacta a los cantores.",
            color = TextTertiary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
