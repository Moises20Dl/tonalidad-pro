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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.AcousticState
import com.example.etymology.EtymologyRepository
import com.example.etymology.GlossaryTerm
import com.example.ui.components.LatinHymnCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
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
fun EtymologyScreen(
    acousticState: AcousticState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedMidi by remember {
        mutableIntStateOf(acousticState.currentPitch?.midiNote ?: 60)
    }

    // Keep in sync with live pitch if singing
    val livePitch = acousticState.currentPitch
    val effectiveMidi = livePitch?.midiNote ?: selectedMidi
    val noteDetail = EtymologyRepository.getNoteHistory(effectiveMidi, acousticState.referenceA4)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf("Todas") + EtymologyRepository.GLOSSARY.map { it.category }.distinct()
    }

    val filteredGlossary = remember(searchQuery, selectedCategory) {
        EtymologyRepository.GLOSSARY.filter { item ->
            val matchesCategory = selectedCategory == null || selectedCategory == "Todas" || item.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                item.term.contains(searchQuery, ignoreCase = true) ||
                item.etymology.contains(searchQuery, ignoreCase = true) ||
                item.acousticTheory.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .testTag("etymology_screen")
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ETIMOLOGÍA Y ANÁLISIS ACÚSTICO",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Origen histórico de los nombres de notas, neumas medievales y glosario metodológico.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Chromatic Note Selector Wheel
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "EXPLORAR NOTA MUSICAL",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scroll of 12 chromatic pitches (octave 4)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val baseMidis = 60..71 // C4 to B4
                    items(baseMidis.toList()) { midi ->
                        val isSelected = (effectiveMidi % 12) == (midi % 12)
                        val info = EtymologyRepository.getNoteHistory(midi)

                        Surface(
                            color = if (isSelected) AccentGold.copy(alpha = 0.2f) else BgInset,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSelected) AccentGold else BorderDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMidi = midi }
                                .testTag("btn_select_note_$midi")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = info.solfa,
                                    color = if (isSelected) AccentGold else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = info.letter,
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Note Detailed Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCardElevated),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderHighlight, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${noteDetail.solfa} (${noteDetail.letter})",
                            color = AccentGold,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif
                        )
                        if (noteDetail.enharmonicName != null) {
                            Text(
                                text = "Enarmónico: ${noteDetail.enharmonicName}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.2f".format(noteDetail.frequencyHz)} Hz",
                            color = AccentTeal,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MIDI ${noteDetail.midiNote} · Octava ${noteDetail.octave}",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Origin of Syllable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgInset, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ORIGEN DE LA SÍLABA DE SOLFEO",
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = noteDetail.syllableOrigin,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Serif
                    )
                }

                // Origin of Latin Letter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgInset, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "NOTACIÓN ALFABÉTICA BOECIANA",
                        color = AccentTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = noteDetail.letterOrigin,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Serif
                    )
                }

                // Accidental history if sharp
                if (noteDetail.accidentalHistory != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgInset, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ALTERACIÓN CROMÁTICA (SOSTENIDO / BEMOL)",
                            color = AccentOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = noteDetail.accidentalHistory,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                // Special Curiosity
                if (noteDetail.acousticCuriosity != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "★ ${noteDetail.acousticCuriosity}",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Latin Hymn View
        LatinHymnCard(activeHymnIndex = noteDetail.hymnIndex)

        // Comprehensive Glossary of Musical Acoustics
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "GLOSARIO METODOLÓGICO Y ACÚSTICA PROFESIONAL",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar término o etimología...", color = TextTertiary, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedContainerColor = BgInset,
                        unfocusedContainerColor = BgInset
                    ),
                    singleLine = true
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = (selectedCategory == category) || (selectedCategory == null && category == "Todas")
                        Surface(
                            color = if (isSelected) AccentTeal else BgInset,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable { selectedCategory = category }
                                .border(1.dp, if (isSelected) AccentTeal else BorderDark, RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) BgInset else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Term Cards
                filteredGlossary.forEach { item ->
                    GlossaryItemView(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GlossaryItemView(item: GlossaryTerm) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgInset, RoundedCornerShape(10.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.term,
                color = AccentGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Surface(
                color = BgCardElevated,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.category,
                    color = AccentTeal,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "Etimología: ${item.etymology}",
            color = TextSecondary,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )

        Text(
            text = "Teoría Acústica: ${item.acousticTheory}",
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCardElevated, RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "Práctica: ${item.practicalApplication}",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
