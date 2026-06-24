package com.ace.mobile.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.domain.usecase.stats.GetOfficialStatsUseCase
import com.ace.mobile.domain.usecase.stats.ReconcileStatsUseCase
import com.ace.shared.dto.ClientStatsDto
import com.ace.shared.dto.StatsResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getOfficialStatsUseCase: GetOfficialStatsUseCase,
    private val reconcileStatsUseCase: ReconcileStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getOfficialStatsUseCase(forceRefresh).collect { result ->
                result.onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(
                        stats = stats,
                        isLoading = false,
                        error = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun reconcile(clientStats: ClientStatsDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReconciling = true)
            val result = reconcileStatsUseCase(clientStats)
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isReconciling = false,
                    reconcileResult = response,
                    // Si hay discrepancias, actualizar stats con los oficiales
                    stats = if (response.discrepancies.isNotEmpty()) {
                        StatsResponseDto(
                            totalXp = response.officialStats.officialTotalXp,
                            totalSessions = response.officialStats.officialTotalSessions,
                            totalBlocks = response.officialStats.officialTotalBlocks,
                            totalDurationSeconds = response.officialStats.officialTotalDurationSeconds,
                            avgBpmAllTime = response.officialStats.officialAvgBpmAllTime,
                            currentRank = response.currentRank,
                            nextRank = null,
                            xpToNextRank = null
                        )
                    } else _uiState.value.stats
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isReconciling = false,
                    error = error.message ?: "Error en reconcile"
                )
            }
        }
    }

    data class StatsUiState(
        val stats: StatsResponseDto? = null,
        val isLoading: Boolean = false,
        val isReconciling: Boolean = false,
        val reconcileResult: com.ace.shared.dto.StatsReconcileResponseDto? = null,
        val error: String? = null
    )
}