package com.ace.mobile.feature.ranking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.ace.mobile.core.ui.components.AceBottomNav
import com.ace.mobile.core.ui.components.AceButtonFilled
import com.ace.mobile.core.ui.components.AceCard
import com.ace.mobile.core.ui.components.AceTab
import com.ace.mobile.core.ui.theme.AceColors
import com.ace.mobile.core.ui.theme.AceTypography
import com.ace.mobile.feature.auth.presentation.AuthBackground
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    navController: NavController,
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
                title = {
                    Text(
                        text = "RANKING",
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
        },
        bottomBar = {
            AceBottomNav(
                selectedTab = AceTab.RANKING,
                onTabSelected = { tab ->
                    when (tab) {
                        AceTab.HOME -> navController.navigate("home_screen_route") {
                            popUpTo("home_screen_route") { inclusive = true }
                        }
                        AceTab.EXERCISE -> navController.navigate("session_screen_route")
                        AceTab.RANKING -> { /* ya estamos */ }
                        AceTab.STATS -> navController.navigate("stats_screen_route")
                        AceTab.PROFILE -> navController.navigate("profile_screen_route")
                    }
                }
            )
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
            ) {
                // ── TabRow ─────────────────────────────────────────────────
                Surface(
                    color = AceColors.CardBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RankingViewModel.RankingTab.entries.forEach { tab ->
                            val isSelected = tab == selectedTab
                            val label = when (tab) {
                                RankingViewModel.RankingTab.GLOBAL -> "Global"
                                RankingViewModel.RankingTab.MUNICIPAL -> "Municipal"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { viewModel.selectTab(tab) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AceColors.NeonRed else AceColors.TextMuted
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(3.dp)
                                                .background(AceColors.NeonRed, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Header + Timestamp stale ───────────────────────────────
                if (cacheAgeMinutes > 60 && uiState is RankingViewModel.RankingUiState.Success) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Actualizado hace ${cacheAgeMinutes / 60}h ${cacheAgeMinutes % 60}m",
                            fontSize = 10.sp,
                            color = AceColors.TextMuted
                        )
                    }
                }

                // ── Content ────────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is RankingViewModel.RankingUiState.Loading -> {
                            if (!isRefreshing) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AceColors.NeonRed,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(40.dp)
                                    )
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
                            RankingContent(
                                data = state.data,
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refresh() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CONTENIDO DEL RANKING
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingContent(
    data: RankingResponseDto,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
        // Usamos el parámetro indicator para mantener tus colores personalizados
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = AceColors.NeonRed,
                containerColor = AceColors.CardBg
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ── Posición Propia Destacada ──────────────────────────────
            if (data.myPosition > 0) {
                item {
                    MyPositionCard(
                        position = data.myPosition,
                        username = data.top.find { it.position == data.myPosition }?.username ?: "Tú",
                        totalXp = data.myTotalXp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Lista Top 10 ───────────────────────────────────────────
            items(data.top) { entry ->
                RankingItem(
                    entry = entry,
                    isMe = entry.position == data.myPosition
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = AceColors.BorderDim
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Actualizado: ${data.lastUpdated.take(19)}",
                    fontSize = 10.sp,
                    color = AceColors.TextMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  POSICIÓN PROPIA DESTACADA
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MyPositionCard(
    position: Int,
    username: String,
    totalXp: Long
) {
    AceCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        // Sobreescribimos el borde para que sea más prominente
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AceColors.NeonRed.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                AceColors.NeonRed.copy(alpha = 0.50f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar / Inicial
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AceColors.CardBg)
                            .border(1.dp, AceColors.BorderDim, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.take(1).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AceColors.TextPrimary
                        )
                    }

                    Column {
                        Text(
                            text = username,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AceColors.TextPrimary
                        )
                        Text(
                            text = "#$position",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = AceColors.NeonRed
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalXp",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AceColors.NeonRed
                    )
                    Text(
                        text = "XP",
                        fontSize = 10.sp,
                        color = AceColors.NeonRed.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FILA DE RANKING (TOP 10)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RankingItem(
    entry: RankingEntryDto,
    isMe: Boolean
) {
    val positionColor = when (entry.position) {
        1 -> Color(0xFFFFD700) // Dorado
        2 -> Color(0xFFC0C0C0) // Plata
        3 -> Color(0xFFCD7F32) // Bronce
        else -> AceColors.TextPrimary
    }

    val rowBackground = if (isMe) {
        AceColors.NeonRed.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Número de posición
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.position.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = positionColor
                )
            }

            // Avatar / Inicial
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AceColors.CardBg)
                    .border(1.dp, AceColors.BorderDim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.username.take(1).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AceColors.TextPrimary
                )
            }

            // Nickname
            Text(
                text = entry.username,
                fontSize = 14.sp,
                color = AceColors.TextPrimary,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
            )
        }

        // XP alineado derecha
        Text(
            text = "${entry.totalXp}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AceColors.NeonRed
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ERROR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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