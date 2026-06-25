package com.ace.mobile.feature.ranking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cacheAgeMinutes by viewModel.cacheAgeMinutes.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenVisible()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking", fontWeight = FontWeight.Bold) },
                actions = {
                    // ← Botón de refresh explícito
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar"
                            )
                        }
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
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == RankingViewModel.RankingTab.GLOBAL,
                    onClick = { viewModel.selectTab(RankingViewModel.RankingTab.GLOBAL) },
                    text = { Text("Global") }
                )
                Tab(
                    selected = selectedTab == RankingViewModel.RankingTab.MUNICIPAL,
                    onClick = { viewModel.selectTab(RankingViewModel.RankingTab.MUNICIPAL) },
                    text = { Text("Municipal") }
                )
            }

            // Cache stale indicator
            if (cacheAgeMinutes > 60 && uiState is RankingViewModel.RankingUiState.Success) {
                Text(
                    text = "Actualizado hace ${cacheAgeMinutes / 60}h ${cacheAgeMinutes % 60}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is RankingViewModel.RankingUiState.Loading -> {
                        if (!isRefreshing) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is RankingViewModel.RankingUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.refresh() }
                        )
                    }

                    is RankingViewModel.RankingUiState.Success -> {
                        RankingContent(data = state.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingContent(
    data: RankingResponseDto
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mi posición destacada (sticky)
        if (data.myPosition > 0) {
            MyPositionCard(
                position = data.myPosition,
                totalXp = data.myTotalXp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista top
        Text(
            text = "TOP 10",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data.top) { entry ->
                RankingItem(entry = entry, isMe = entry.position == data.myPosition)
            }
        }

        // Last updated
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Actualizado: ${data.lastUpdated.take(19)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MyPositionCard(position: Int, totalXp: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tú",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "#$position",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "XP Total",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$totalXp",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RankingItem(entry: RankingEntryDto, isMe: Boolean) {
    val medalEmoji = when (entry.position) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }

    val positionColor = when (entry.position) {
        1 -> Color(0xFFFFD700) // Oro
        2 -> Color(0xFFC0C0C0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Posición con medalla
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (entry.position <= 3) positionColor.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (medalEmoji != null) {
                        Text(text = medalEmoji, fontSize = 18.sp)
                    } else {
                        Text(
                            text = entry.position.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = positionColor
                        )
                    }
                }

                // Nombre
                Column {
                    Text(
                        text = entry.username,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                    if (isMe) {
                        Text(
                            text = "Tú",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // XP
            Text(
                text = "${entry.totalXp} XP",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}