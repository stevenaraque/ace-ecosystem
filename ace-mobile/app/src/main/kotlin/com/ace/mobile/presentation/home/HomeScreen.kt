package com.ace.mobile.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ace.mobile.presentation.profile.ProfileEvent
import com.ace.mobile.presentation.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        profileViewModel.event.collect { event ->
            when (event) {
                ProfileEvent.NavigateToLogin -> {
                    navController.navigate("login_screen_route") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
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
                title = { Text("A.C.E", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("profile_screen_route") }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakBanner(
                currentStreak = uiState.currentStreak,
                bestStreak = uiState.bestStreak,
                hasTrainedToday = uiState.hasTrainedToday
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¿Qué quieres hacer hoy?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            HomeActionCard(
                title = "Iniciar Ejercicio",
                subtitle = "Comienza una nueva sesión",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate("session_screen_route") }
            )

            HomeActionCard(
                title = "Ranking",
                subtitle = if (uiState.globalPosition > 0) "Tu posición: #${uiState.globalPosition}" else "Global y Municipal",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { navController.navigate("ranking_screen_route") }
            )

            // ← NUEVO Hito 4: Estadísticas
            HomeActionCard(
                title = "Estadísticas",
                subtitle = "Progreso, historial y logros",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = { navController.navigate("stats_screen_route") }
            )

            HomeActionCard(
                title = "Mi Perfil",
                subtitle = "Configuración y cuenta",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { navController.navigate("profile_screen_route") }
            )

            Spacer(modifier = Modifier.weight(1f))

            QuickStatsRow(
                totalXp = uiState.totalXp,
                totalSessions = uiState.totalSessions,
                globalPosition = uiState.globalPosition
            )
        }
    }
}

@Composable
private fun StreakBanner(
    currentStreak: Int,
    bestStreak: Int,
    hasTrainedToday: Boolean
) {
    val containerColor = if (hasTrainedToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val message = when {
        currentStreak == 0 -> "¡Empieza tu racha hoy! 💪"
        hasTrainedToday -> "🔥 Racha de $currentStreak días (mejor: $bestStreak)"
        else -> "⚠️ ¡Entrena hoy para no perder tu racha de $currentStreak días!"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasTrainedToday) "🔥" else "⚠️",
                style = MaterialTheme.typography.headlineMedium
            )
            Column {
                Text(
                    text = "Racha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (title) {
                    "Iniciar Ejercicio" -> "💪"
                    "Ranking" -> "🏆"
                    "Estadísticas" -> "📊"
                    "Mi Perfil" -> "👤"
                    else -> "•"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    totalXp: Long,
    totalSessions: Int,
    globalPosition: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(label = "XP Total", value = totalXp.toString(), modifier = Modifier.weight(1f))
        StatCard(label = "Sesiones", value = totalSessions.toString(), modifier = Modifier.weight(1f))
        StatCard(label = "Posición", value = "#$globalPosition", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}