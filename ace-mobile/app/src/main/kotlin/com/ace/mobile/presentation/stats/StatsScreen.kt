package com.ace.mobile.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

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
                title = { Text("Estadísticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStats(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading && uiState.stats == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.stats == null -> {
                    ErrorCard(message = uiState.error!!, onRetry = { viewModel.loadStats() })
                }
                uiState.stats != null -> {
                    StatsContent(
                        stats = uiState.stats!!,
                        isReconciling = uiState.isReconciling,
                        onReconcile = {
                            // Ejemplo: enviar stats locales actuales para reconcile
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
        }
    }
}

@Composable
private fun StatsContent(
    stats: com.ace.shared.dto.StatsResponseDto,
    isReconciling: Boolean,
    onReconcile: () -> Unit
) {
    // Rango actual
    RankCard(
        currentRank = stats.currentRank,
        nextRank = stats.nextRank,
        xpToNext = stats.xpToNextRank,
        totalXp = stats.totalXp
    )

    // Grid de estadísticas
    StatsGrid(stats = stats)

    // Botón de reconcile
    Button(
        onClick = onReconcile,
        enabled = !isReconciling,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isReconciling) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sincronizando...")
        } else {
            Text("🔄 Sincronizar con servidor")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆 $currentRank",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalXp XP acumulados",
                style = MaterialTheme.typography.titleMedium
            )
            if (nextRank != null && xpToNext != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { totalXp.toFloat() / (totalXp + xpToNext).toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Faltan $xpToNext XP para $nextRank",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: com.ace.shared.dto.StatsResponseDto) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "XP Total",
                value = stats.totalXp.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Sesiones",
                value = stats.totalSessions.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Bloques",
                value = stats.totalBlocks.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Duración",
                value = formatDuration(stats.totalDurationSeconds),
                modifier = Modifier.weight(1f)
            )
        }
        StatCard(
            label = "BPM Promedio",
            value = String.format("%.1f", stats.avgBpmAllTime),
            modifier = Modifier.fillMaxWidth()
        )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ $message",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}