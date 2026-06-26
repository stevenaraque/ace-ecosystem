package com.ace.mobile.feature.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ace.mobile.core.ui.components.AceBottomNav
import com.ace.mobile.core.ui.components.AceButtonFilled
import com.ace.mobile.core.ui.components.AceCard
import com.ace.mobile.core.ui.components.AceTab
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography
import com.ace.mobile.feature.auth.presentation.AuthBackground
import com.ace.shared.dto.StatsResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MIS ESTADÍSTICAS",
                        style = AceTypography.H2.copy(
                            fontSize = 16.sp,
                            color = AceColors.NeonRed
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AceColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStats(forceRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            tint = AceColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AceColors.BgBlack,
                    scrolledContainerColor = AceColors.BgBlack
                )
            )
        },
        bottomBar = {
            AceBottomNav(
                selectedTab = AceTab.STATS,
                onTabSelected = { tab ->
                    when (tab) {
                        AceTab.HOME -> navController.navigate("home_screen_route") {
                            popUpTo("home_screen_route") { inclusive = true }
                        }
                        AceTab.EXERCISE -> navController.navigate("session_screen_route")
                        AceTab.RANKING -> navController.navigate("ranking_screen_route")
                        AceTab.STATS -> { /* ya estamos aquí */ }
                        AceTab.PROFILE -> navController.navigate("profile_screen_route") // <─── Agrega esto
                    }
                }
            )
        },
        containerColor = AceColors.BgBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AuthBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    uiState.isLoading && uiState.stats == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AceColors.NeonRed,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    uiState.error != null && uiState.stats == null -> {
                        Spacer(modifier = Modifier.height(48.dp))
                        ErrorContent(
                            message = uiState.error!!,
                            onRetry = { viewModel.loadStats() }
                        )
                    }

                    uiState.stats != null -> {
                        StatsContent(
                            stats = uiState.stats!!,
                            isReconciling = uiState.isReconciling,
                            onReconcile = {
                                val currentStats = uiState.stats!!
                                viewModel.reconcile(
                                    com.ace.shared.dto.ClientStatsDto(
                                        totalXp = currentStats.totalXp,
                                        totalSessions = currentStats.totalSessions,
                                        totalBlocks = currentStats.totalBlocks,
                                        totalDurationSeconds = currentStats.totalDurationSeconds,
                                        avgBpmAllTime = currentStats.avgBpmAllTime
                                    )
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.StatsContent(
    stats: StatsResponseDto,
    isReconciling: Boolean,
    onReconcile: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    // ── Sección 1: Stats Globales (Grid 2×2) ───────────────────────
    AceCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactStatCard(
                    label = "XP TOTAL",
                    value = stats.totalXp.toString(),
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = "SESIONES",
                    value = stats.totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactStatCard(
                    label = "DURACIÓN",
                    value = formatDurationHours(stats.totalDurationSeconds),
                    modifier = Modifier.weight(1f)
                )
                CompactStatCard(
                    label = "AVG BPM",
                    value = String.format("%.0f", stats.avgBpmAllTime),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Texto de sync
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isReconciling) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = AceColors.NeonRed,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isReconciling) "Sincronizando con servidor..." else "Sincronizado",
            fontSize = 10.sp,
            color = AceColors.TextMuted
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Sección 2: Rango / Progreso ────────────────────────────────
    RankCard(
        currentRank = stats.currentRank,
        nextRank = stats.nextRank,
        xpToNext = stats.xpToNextRank,
        totalXp = stats.totalXp
    )

    Spacer(modifier = Modifier.height(24.dp))

    // ── Sección 3: Racha (placeholder — conectar con HomeViewModel) ──
    // TODO: Inyectar HomeViewModel o pasar currentStreak/bestStreak como parámetros
    AceCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakItem(value = "—", label = "RACHA ACTUAL") // TODO: reemplazar con dato real
            StreakItem(value = "—", label = "MEJOR RACHA")   // TODO: reemplazar con dato real
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Sección 4: Historial (placeholder — conectar con S9) ─────────
    // TODO: Cuando el backend/ViewModel exponga historial de sesiones, reemplazar esto
    Text(
        text = "ÚLTIMAS SESIONES",
        style = AceTypography.H2.copy(
            fontSize = 14.sp,
            color = AceColors.TextPrimary
        ),
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(12.dp))
    AceCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Historial próximamente",
                fontSize = 13.sp,
                color = AceColors.TextMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Botón Reconcile ────────────────────────────────────────────
    AceButtonFilled(
        text = if (isReconciling) "SINCRONIZANDO..." else "SINCRONIZAR CON SERVIDOR",
        onClick = onReconcile,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        isLoading = isReconciling,
        textStyle = AceTypography.H1.copy(
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = androidx.compose.ui.graphics.Color.White
        )
    )

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun CompactStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AceColors.CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, AceColors.BorderDim),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AceColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = AceColors.NeonRed.copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun RankCard(
    currentRank: String,
    nextRank: String?,
    xpToNext: Long?,
    totalXp: Long
) {
    AceCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RANGO ACTUAL",
                fontSize = 10.sp,
                color = AceColors.NeonRed.copy(alpha = 0.75f),
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentRank.uppercase(),
                style = AceTypography.H2.copy(
                    fontSize = 22.sp,
                    color = AceColors.TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalXp XP acumulados",
                fontSize = 14.sp,
                color = AceColors.TextSecondary
            )

            if (nextRank != null && xpToNext != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = totalXp.toFloat() / (totalXp + xpToNext).toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AceColors.NeonRed,
                    trackColor = AceColors.BorderDim,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Faltan $xpToNext XP para $nextRank",
                    fontSize = 11.sp,
                    color = AceColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun StreakItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔥",
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AceColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = AceColors.NeonRed.copy(alpha = 0.75f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = AceColors.NeonRed
        )
        Spacer(modifier = Modifier.height(24.dp))
        AceButtonFilled(
            text = "REINTENTAR",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

private fun formatDurationHours(seconds: Long): String {
    val hours = seconds / 3600
    return if (hours > 0) "${hours}h" else "${seconds / 60}m"
}