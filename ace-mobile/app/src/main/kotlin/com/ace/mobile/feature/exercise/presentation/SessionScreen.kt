package com.ace.mobile.feature.exercise.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ace.mobile.core.ui.components.AceBottomNav
import com.ace.mobile.core.ui.components.AceButtonFilled
import com.ace.mobile.core.ui.components.AceButtonOutlined
import com.ace.mobile.core.ui.components.AceCard
import com.ace.mobile.core.ui.components.AceTab
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography
import com.ace.mobile.feature.auth.presentation.AuthBackground
import com.ace.shared.enums.SportType
import sena.adso.ace_mobile.BuildConfig
import java.util.Locale

// ─── Mapeo de deportes a iconos y nombres visuales ───────────────────────────
private val SportType.displayName: String
    get() = when (this) {
        SportType.RUNNING -> "Running"
        SportType.CYCLING -> "Cycling"
        SportType.WALKING -> "Walking"

    }

private val SportType.icon: ImageVector
    get() = when (this) {
        SportType.RUNNING -> Icons.Default.Check
        SportType.CYCLING -> Icons.Default.Check
        SportType.WALKING -> Icons.Default.Check

    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    userId: String,
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val blockCount by viewModel.blockCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    // Estado local para selección de deporte en Idle
    var selectedSport by remember { mutableStateOf<SportType?>(null) }

    // Animación de pulso de FC (cada vez que cambia el valor)
    val pulseScale by animateFloatAsState(
        targetValue = if (heartRate > 0) 1.05f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "HeartPulse"
    )

    // Determinar si mostrar BottomNav (solo en estados no inmersivos)
    val showBottomNav = uiState is SessionUiState.Idle ||
            uiState is SessionUiState.Completed ||
            uiState is SessionUiState.Error

    Scaffold(
        topBar = {
            // Solo mostrar TopAppBar en Idle (selección). En sesión activa no hay AppBar.
            if (uiState is SessionUiState.Idle) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Ejercicio",
                            style = AceTypography.H2.copy(
                                fontSize = 22.sp,
                                color = AceColors.TextPrimary
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AceColors.BgBlack,
                        scrolledContainerColor = AceColors.BgBlack
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomNav) {
                AceBottomNav(
                    selectedTab = AceTab.EXERCISE,
                    onTabSelected = { tab ->
                        when (tab) {
                            AceTab.HOME -> navController.navigate("home_screen_route") {
                                popUpTo("home_screen_route") { inclusive = true }
                            }
                            AceTab.EXERCISE -> { /* ya estamos */ }
                            AceTab.RANKING -> navController.navigate("ranking_screen_route")
                            AceTab.STATS -> navController.navigate("stats_screen_route")
                            AceTab.PROFILE -> navController.navigate("profile_screen_route")
                        }
                    }
                )
            }
        },
        containerColor = AceColors.BgBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Capas 1–5: Fondo animado A.C.E
            AuthBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(
                        if (uiState !is SessionUiState.Active && uiState !is SessionUiState.Paused) {
                            Modifier.padding(horizontal = 20.dp)
                        } else Modifier
                    )
                    .then(
                        if (uiState is SessionUiState.Idle || uiState is SessionUiState.Completed || uiState is SessionUiState.Error) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {
                    is SessionUiState.Idle -> {
                        SessionIdleContent(
                            isConnected = isConnected,
                            isSimulating = isSimulating,
                            selectedSport = selectedSport,
                            onSelectSport = { selectedSport = it },
                            onStartSession = { sport ->
                                viewModel.startSession(sport, userId)
                            },
                            onToggleSimulation = { viewModel.toggleSimulation() }
                        )
                    }

                    is SessionUiState.Loading -> {
                        SessionLoadingContent()
                    }

                    is SessionUiState.Active -> {
                        SessionActiveContent(
                            session = state.session,
                            heartRate = heartRate,
                            pulseScale = pulseScale,
                            elapsedSeconds = elapsedSeconds,
                            blockCount = blockCount,
                            totalXp = totalXp,
                            isConnected = isConnected,
                            isSimulating = isSimulating,
                            onToggleSimulation = { viewModel.toggleSimulation() },
                            onPause = { viewModel.pauseSession() },
                            onStop = { viewModel.stopSession() }
                        )
                    }

                    is SessionUiState.Paused -> {
                        SessionPausedContent(
                            session = state.session,
                            heartRate = heartRate,
                            pulseScale = pulseScale,
                            elapsedSeconds = elapsedSeconds,
                            blockCount = blockCount,
                            totalXp = totalXp,
                            isConnected = isConnected,
                            isAutoPaused = state.isAutoPaused,
                            isSimulating = isSimulating,
                            onToggleSimulation = { viewModel.toggleSimulation() },
                            onResume = { viewModel.resumeSession() },
                            onStop = { viewModel.stopSession() }
                        )
                    }

                    is SessionUiState.Stopping -> {
                        SessionStoppingContent()
                    }

                    is SessionUiState.Completed -> {
                        SessionCompletedContent(
                            session = state.session,
                            xpGained = state.xpGained,
                            blocksInSession = state.blocksInSession,
                            onReset = { viewModel.resetState() },
                            navController = navController
                        )
                    }

                    is SessionUiState.Error -> {
                        SessionErrorContent(
                            message = state.message,
                            onRetry = { viewModel.resetState() }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: IDLE (Selección de Deporte) — §5.3
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionIdleContent(
    isConnected: Boolean,
    isSimulating: Boolean,
    selectedSport: SportType?,
    onSelectSport: (SportType) -> Unit,
    onStartSession: (SportType) -> Unit,
    onToggleSimulation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Subtítulo
        Text(
            text = "Selecciona deporte e inicia",
            fontSize = 12.sp,
            color = AceColors.TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Estado Wear OS
        WearOsStatusCard(
            isConnected = isConnected,
            isSimulating = isSimulating,
            onToggleSimulation = onToggleSimulation
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Selector de deporte: Grid 2 columnas
        val sports = SportType.values().toList()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sports.chunked(2).forEach { rowSports ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSports.forEach { sport ->
                        val isSelected = selectedSport == sport
                        SportSelectorCard(
                            sport = sport,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectSport(sport) }
                        )
                    }
                    // Rellenar si la fila tiene 1 elemento
                    if (rowSports.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón INICIAR (sticky visual, aunque aquí está al final del scroll)
        val canStart = selectedSport != null && isConnected
        AceButtonFilled(
            text = "INICIAR",
            onClick = { selectedSport?.let { onStartSession(it) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canStart,
            textStyle = AceTypography.H1.copy(
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                color = Color.White
            )
        )

        // Texto descriptivo si está deshabilitado
        if (!canStart) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    selectedSport == null -> "Selecciona un deporte para continuar"
                    !isConnected -> "Conecta el reloj para iniciar"
                    else -> ""
                },
                fontSize = 11.sp,
                color = AceColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WearOsStatusCard(
    isConnected: Boolean,
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit
) {
    AceCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador de conexión (punto 10dp)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isConnected) AceColors.SuccessGreen else AceColors.WarningYellow,
                            shape = RoundedCornerShape(50)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Reloj conectado" else "Reloj desconectado",
                    fontSize = 12.sp,
                    color = AceColors.TextSecondary
                )
            }

            if (BuildConfig.DEBUG) {
                TextButton(onClick = onToggleSimulation) {
                    Text(
                        text = if (isSimulating) "Detener simulación" else "Simular",
                        fontSize = 12.sp,
                        color = AceColors.NeonRed.copy(alpha = 0.80f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SportSelectorCard(
    sport: SportType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AceColors.NeonRed else AceColors.BorderDim
    val backgroundColor = if (isSelected) AceColors.NeonRed.copy(alpha = 0.12f) else AceColors.CardBg

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = sport.icon,
                contentDescription = sport.displayName,
                tint = AceColors.TextPrimary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sport.displayName,
                fontSize = 11.sp,
                color = AceColors.TextPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: LOADING
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AceColors.NeonRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Inicializando sesión...",
                fontSize = 14.sp,
                color = AceColors.TextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: ACTIVE (Sesión Activa) — §5.4
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionActiveContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    pulseScale: Float,
    elapsedSeconds: Int,
    blockCount: Int,
    totalXp: Double,
    isConnected: Boolean,
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Chip Foreground Service
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AceColors.NeonRed.copy(alpha = 0.20f),
            border = BorderStroke(1.dp, AceColors.NeonRed.copy(alpha = 0.60f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.height(32.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A.C.E — Sesión activa",
                    fontSize = 12.sp,
                    color = AceColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header: Deporte + Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.sportType.name.uppercase(),
                style = AceTypography.H2.copy(
                    fontSize = 14.sp,
                    color = AceColors.NeonRed
                )
            )
            Text(
                text = formatTime(elapsedSeconds),
                style = AceTypography.H2.copy(
                    fontSize = 18.sp,
                    color = AceColors.TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Display de FC (Héroe visual)
        Row(
            modifier = Modifier.scale(pulseScale),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${heartRate.toInt()}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = AceColors.TextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "BPM",
                fontSize = 16.sp,
                color = AceColors.NeonRed,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Indicador de conexión junto al número
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(10.dp)
                    .background(
                        color = if (isConnected) AceColors.SuccessGreen else AceColors.WarningYellow,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta Bloque Actual
        AceCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Bloque actual",
                    style = AceTypography.H3.copy(fontSize = 14.sp),
                    color = AceColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Timer de bloque (placeholder: usamos tiempo global como proxy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(elapsedSeconds),
                        fontSize = 24.sp,
                        color = AceColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ 01:00",
                        fontSize = 16.sp,
                        color = AceColors.TextMuted
                    )
                }

                // Barra de progreso (placeholder)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (elapsedSeconds % 60) / 60f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AceColors.NeonRed,
                    trackColor = AceColors.BorderDim,
                    drawStopIndicator = {}
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats del bloque
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BlockStat(label = "Avg BPM", value = "${heartRate.toInt()}")
                    BlockStat(label = "Bloques", value = "$blockCount")
                    BlockStat(label = "Muestras", value = "—") // TODO: exponer desde ViewModel
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de bloques completados (simplificada: solo conteo + XP)
        if (blockCount > 0) {
            AceCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bloques completados",
                        fontSize = 14.sp,
                        color = AceColors.TextPrimary
                    )
                    Text(
                        text = "+${String.format(Locale.getDefault(), "%.1f", totalXp)} XP",
                        fontSize = 16.sp,
                        color = AceColors.NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón de simulación en Debug
        if (BuildConfig.DEBUG) {
            AceButtonOutlined(
                text = if (isSimulating) "DETENER SIMULACIÓN" else "SIMULAR WEAR OS",
                onClick = onToggleSimulation,
                modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Controles de sesión (fijos al fondo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AceButtonOutlined(
                text = "PAUSAR",
                onClick = onPause,
                modifier = Modifier.weight(1f).height(52.dp)
            )
            AceButtonFilled(
                text = "TERMINAR",
                onClick = onStop,
                modifier = Modifier.weight(1f).height(52.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: PAUSED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionPausedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    pulseScale: Float,
    elapsedSeconds: Int,
    blockCount: Int,
    totalXp: Double,
    isConnected: Boolean,
    isAutoPaused: Boolean,
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (isAutoPaused) "Auto-Pausa" else "Sesión Pausada",
            style = AceTypography.H2.copy(
                fontSize = 20.sp,
                color = AceColors.WarningYellow
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = session.sportType.name.uppercase(),
            style = AceTypography.H2.copy(
                fontSize = 14.sp,
                color = AceColors.NeonRed
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer en gris
        Text(
            text = formatTime(elapsedSeconds),
            style = AceTypography.H2.copy(
                fontSize = 48.sp,
                color = AceColors.TextMuted
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats resumidos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BlockStat(label = "Última FC", value = "${heartRate.toInt()}", unit = "BPM")
            BlockStat(label = "Bloques", value = "$blockCount")
            BlockStat(label = "XP", value = String.format(Locale.getDefault(), "%.1f", totalXp))
        }

        Spacer(modifier = Modifier.weight(1f))

        if (BuildConfig.DEBUG) {
            AceButtonOutlined(
                text = if (isSimulating) "DETENER SIMULACIÓN" else "SIMULAR WEAR OS",
                onClick = onToggleSimulation,
                modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AceButtonOutlined(
                text = "REANUDAR",
                onClick = onResume,
                modifier = Modifier.weight(1f).height(52.dp)
            )
            AceButtonFilled(
                text = "TERMINAR",
                onClick = onStop,
                modifier = Modifier.weight(1f).height(52.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: STOPPING
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionStoppingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AceColors.NeonRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cerrando bloque final y guardando sesión...",
                fontSize = 14.sp,
                color = AceColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: COMPLETED (Resumen Post-Sesión) — §5.5
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionCompletedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    xpGained: Double,
    blocksInSession: Int,
    onReset: () -> Unit,
    navController: NavController
) {
    // Animación de counter de XP
    val xpAnimated = remember { Animatable(0f) }
    LaunchedEffect(xpGained) {
        xpAnimated.animateTo(
            targetValue = xpGained.toFloat(),
            animationSpec = tween(1500, easing = LinearEasing)
        )
    }

    val durationSeconds = session.timestampEnd?.let { end ->
        ((end - session.timestampStart) / 1000).toInt()
    } ?: 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header celebratorio
        Text(
            text = "¡Sesión Completada!",
            style = AceTypography.H2.copy(
                fontSize = 26.sp,
                color = AceColors.TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${session.sportType.displayName} · ${formatTime(durationSeconds)}",
            fontSize = 14.sp,
            color = AceColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // XP Total (Héroe visual)
        Text(
            text = "${String.format(Locale.getDefault(), "%.0f", xpAnimated.value)}",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = AceColors.NeonRed
        )
        Text(
            text = "XP GANADOS",
            fontSize = 11.sp,
            color = AceColors.NeonRed.copy(alpha = 0.75f),
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats de sesión (Grid 2×2)
        AceCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), padding = 16.dp) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItemSummary(label = "Duración", value = formatTime(durationSeconds))
                    StatItemSummary(label = "Avg BPM", value = "—") // TODO
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItemSummary(label = "Bloques", value = "$blocksInSession")
                    StatItemSummary(label = "Muestras", value = "—") // TODO
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Desglose por bloque (simplificado)
        if (blocksInSession > 0) {
            AceCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total de bloques",
                        fontSize = 14.sp,
                        color = AceColors.TextPrimary
                    )
                    Text(
                        text = "+${String.format(Locale.getDefault(), "%.1f", xpGained)} XP",
                        fontSize = 16.sp,
                        color = AceColors.NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón volver
        AceButtonFilled(
            text = "VOLVER AL INICIO",
            onClick = {
                onReset()
                navController.navigate("home_screen_route") {
                    popUpTo("home_screen_route") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            textStyle = AceTypography.H1.copy(
                fontSize = 14.sp,
                letterSpacing = 3.sp,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ESTADO: ERROR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SessionErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Error",
            style = AceTypography.H2.copy(
                fontSize = 24.sp,
                color = AceColors.NeonRed
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = AceColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        AceButtonFilled(
            text = "REINTENTAR",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun BlockStat(
    label: String,
    value: String,
    unit: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = AceColors.TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                color = AceColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = AceColors.TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItemSummary(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}