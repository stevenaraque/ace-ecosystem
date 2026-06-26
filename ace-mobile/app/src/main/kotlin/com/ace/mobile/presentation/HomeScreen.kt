// Location: com/ace/mobile/presentation/HomeScreen.kt
package com.ace.mobile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ace.mobile.core.ui.components.*
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography
import com.ace.mobile.feature.auth.presentation.AuthBackground
import com.ace.mobile.feature.profile.presentation.ProfileEvent
import com.ace.mobile.feature.profile.presentation.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        profileViewModel.event.collect { event ->
            when (event) {
                ProfileEvent.NavigateToLogin -> {
                    navController.navigate("login_screen_route") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "A.C.E",
                        style = AceTypography.H2.copy(
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = AceColors.TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile_screen_route") }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = AceColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notificaciones */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
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
                selectedTab = AceTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        AceTab.HOME -> { /* ya estamos aquí */ }
                        AceTab.EXERCISE -> navController.navigate("session_screen_route")
                        AceTab.RANKING -> navController.navigate("ranking_screen_route")
                        AceTab.STATS -> navController.navigate("stats_screen_route")
                        AceTab.PROFILE -> navController.navigate("profile_screen_route")
                    }
                }
            )
        },
        containerColor = AceColors.BgBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Capa 1–5: Fondo animado A.C.E (retícula + cubos + partículas + viñeta)
            AuthBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── 1. Banner de Racha (condicional) ─────────────────────────
                if (uiState.currentStreak > 0) {
                    val isStreakInDanger = !uiState.hasTrainedToday
                    AceBanner(
                        message = if (isStreakInDanger) {
                            "¡Ejercita hoy para no perder tu racha de ${uiState.currentStreak} días!"
                        } else {
                            "🔥 Racha de ${uiState.currentStreak} días"
                        },
                        backgroundColor = if (isStreakInDanger) {
                            AceColors.WarningYellow.copy(alpha = 0.15f)
                        } else {
                            AceColors.NeonRed.copy(alpha = 0.15f)
                        },
                        borderColor = if (isStreakInDanger) {
                            AceColors.WarningYellow.copy(alpha = 0.40f)
                        } else {
                            AceColors.NeonRed.copy(alpha = 0.40f)
                        },
                        contentColor = AceColors.TextPrimary,
                        icon = if (isStreakInDanger) "⚠️" else "🔥"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── 2. Banner de Sync Error (condicional) ────────────────────
                // TODO: Conectar a uiState cuando el ViewModel exponga bloques en error
                // if (uiState.pendingSyncBlocks > 0) {
                //     AceBanner(
                //         message = "X bloques sin sincronizar",
                //         backgroundColor = AceColors.WarningYellow.copy(alpha = 0.15f),
                //         borderColor = AceColors.WarningYellow.copy(alpha = 0.40f),
                //         icon = "⚠️",
                //         onClick = { navController.navigate("diagnostic_sync_route") }
                //     )
                //     Spacer(modifier = Modifier.height(8.dp))
                // }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 3. Quick Stats ───────────────────────────────────────────
                AceQuickStats(
                    totalXp = uiState.totalXp.toString(),
                    globalPosition = if (uiState.globalPosition > 0) "#${uiState.globalPosition}" else "-",
                    totalSessions = uiState.totalSessions.toString()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── 4. CTA Principal: INICIAR EJERCICIO ──────────────────────
                AceButtonFilled(
                    text = "INICIAR EJERCICIO",
                    onClick = { navController.navigate("session_screen_route") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    textStyle = AceTypography.H2.copy(
                        fontSize = 14.sp,
                        letterSpacing = 3.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── 5. Accesos Directos (Grid 2×2) ───────────────────────────
                val shortcuts = listOf(
                    ShortcutData("Ranking", Icons.Default.ThumbUp) {
                        navController.navigate("ranking_screen_route")
                    },
                    ShortcutData("Estadísticas", Icons.Default.Menu) {
                        navController.navigate("stats_screen_route")
                    },
                    ShortcutData("Historial", Icons.Default.DateRange) { /* TODO */ },
                    ShortcutData("Reloj", Icons.Default.AddCircle) { /* TODO */ }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    shortcuts.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { shortcut ->
                                AceShortcutCard(
                                    label = shortcut.label,
                                    icon = shortcut.icon,
                                    modifier = Modifier.weight(1f),
                                    onClick = shortcut.onClick
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─── Datos auxiliares para el grid ────────────────────────────────────────────
private data class ShortcutData(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// ─── Tarjeta compacta de acceso directo (especificación §5.2) ─────────────────
@Composable
private fun AceShortcutCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = AceColors.CardBg,
        border = BorderStroke(1.dp, AceColors.BorderDim),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AceColors.TextPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = AceColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}