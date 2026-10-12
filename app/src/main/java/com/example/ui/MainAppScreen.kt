package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.dsp.AcousticEngine
import com.example.dsp.NotationMode
import com.example.ui.screens.AcousticAnalyzerScreen
import com.example.ui.screens.EtymologyScreen
import com.example.ui.screens.SheetMusicScreen
import com.example.ui.screens.ToneGeneratorScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgInset
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class AppTab(val title: String) {
    ANALYZER("Analizador"),
    SHEET_MUSIC("Partituras"),
    ETYMOLOGY("Etimología"),
    TONE_GEN("Diapasón")
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val acousticEngine = remember { AcousticEngine(coroutineScope) }
    val acousticState by acousticEngine.state.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.ANALYZER) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            acousticEngine.startListening()
        }
    }

    // Auto-start listening if permission already granted
    LaunchedEffect(hasMicPermission) {
        if (hasMicPermission && !acousticState.isRunning) {
            acousticEngine.startListening()
        }
    }

    // Release audio record when screen leaves
    DisposableEffect(Unit) {
        onDispose {
            acousticEngine.stopListening()
        }
    }

    // Back handling: return to ANALYZER if on another tab
    BackHandler(enabled = currentTab != AppTab.ANALYZER) {
        currentTab = AppTab.ANALYZER
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        containerColor = BgDark,
        topBar = {
            AppHeader(
                isRunning = acousticState.isRunning,
                currentTab = currentTab
            )
        },
        bottomBar = {
            AppBottomNav(
                currentTab = currentTab,
                onSelectTab = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.ANALYZER -> {
                    AcousticAnalyzerScreen(
                        state = acousticState,
                        hasRecordPermission = hasMicPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onToggleMic = {
                            if (acousticState.isRunning) {
                                acousticEngine.stopListening()
                            } else {
                                acousticEngine.startListening()
                            }
                        },
                        onStartCalibration = { acousticEngine.startCalibration() },
                        onResetBuffer = { acousticEngine.resetPitchTracker() },
                        onSensitivityChange = { acousticEngine.setSensitivity(it) },
                        onReferenceA4Change = { acousticEngine.setReferenceA4(it) },
                        onNotationModeChange = { acousticEngine.setNotationMode(it) },
                        onNavigateToEtymology = { currentTab = AppTab.ETYMOLOGY }
                    )
                }

                AppTab.SHEET_MUSIC -> {
                    SheetMusicScreen()
                }

                AppTab.ETYMOLOGY -> {
                    EtymologyScreen(acousticState = acousticState)
                }

                AppTab.TONE_GEN -> {
                    ToneGeneratorScreen(referenceA4 = acousticState.referenceA4)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    isRunning: Boolean,
    currentTab: AppTab
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderHighlight, RoundedCornerShape(10.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tonalia_logo),
                        contentDescription = "Tonalia Pro Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tonalia",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = AccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Suite Acústica & Notación",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            // Live status badge
            Surface(
                color = if (isRunning) AccentGreen.copy(alpha = 0.15f) else BgCardElevated,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(
                    1.dp,
                    if (isRunning) AccentGreen.copy(alpha = 0.5f) else BorderDark,
                    RoundedCornerShape(20.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isRunning) AccentGreen else AccentOrange,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "DSP ACTIVO" else "EN ESPERA",
                        color = if (isRunning) AccentGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderDark)
        )
    }
}

@Composable
private fun AppBottomNav(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = BgCard,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(1.dp, BorderDark)
    ) {
        val items = listOf(
            Triple(AppTab.ANALYZER, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
            Triple(AppTab.SHEET_MUSIC, Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
            Triple(AppTab.ETYMOLOGY, Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            Triple(AppTab.TONE_GEN, Icons.Filled.VolumeUp, Icons.Outlined.VolumeUp)
        )

        items.forEach { (tab, filledIcon, outlinedIcon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentTeal,
                    selectedTextColor = AccentTeal,
                    indicatorColor = AccentTeal.copy(alpha = 0.18f),
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary
                ),
                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
            )
        }
    }
}
