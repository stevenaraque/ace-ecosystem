package com.ace.mobile.feature.ranking.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.core.datastore.UserPreferencesDataStore
import com.ace.mobile.feature.ranking.data.RankingCacheRepository
import com.ace.shared.dto.RankingResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RankingViewModel"

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingCacheRepository: RankingCacheRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(RankingTab.GLOBAL)
    val selectedTab: StateFlow<RankingTab> = _selectedTab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ← NUEVO: Indicador de cache stale (>1h)
    private val _cacheAgeMinutes = MutableStateFlow(0L)
    val cacheAgeMinutes: StateFlow<Long> = _cacheAgeMinutes.asStateFlow()

    private var globalData: RankingResponseDto? = null
    private var municipalData: RankingResponseDto? = null
    private var lastLoadTime: Long = 0

    init {
        // ← NUEVO: Escuchar señales de invalidación desde sync
        viewModelScope.launch {
            rankingCacheRepository.invalidateSignal.collectLatest {
                Log.i(TAG, "Cache invalidated, forcing refresh")
                refreshCurrentTab()
            }
        }
        loadGlobalRanking()
    }

    fun selectTab(tab: RankingTab) {
        _selectedTab.value = tab
        when (tab) {
            RankingTab.GLOBAL -> {
                if (globalData != null && !isCacheStale()) {
                    _uiState.value = RankingUiState.Success(globalData!!)
                } else {
                    loadGlobalRanking()
                }
            }
            RankingTab.MUNICIPAL -> {
                if (municipalData != null && !isCacheStale()) {
                    _uiState.value = RankingUiState.Success(municipalData!!)
                } else {
                    loadMunicipalRanking()
                }
            }
        }
    }

    /**
     * Pull-to-refresh explícito del usuario.
     */
    fun refresh() {
        _isRefreshing.value = true
        when (_selectedTab.value) {
            RankingTab.GLOBAL -> loadGlobalRanking(forceRefresh = true)
            RankingTab.MUNICIPAL -> loadMunicipalRanking(forceRefresh = true)
        }
    }

    /**
     * Llamar cuando la pantalla vuelve a primer plano (Lifecycle STARTED).
     */
    fun onScreenVisible() {
        if (isCacheStale()) {
            Log.d(TAG, "Cache stale, auto-refreshing")
            refreshCurrentTab()
        }
    }

    private fun refreshCurrentTab() {
        when (_selectedTab.value) {
            RankingTab.GLOBAL -> loadGlobalRanking(forceRefresh = true)
            RankingTab.MUNICIPAL -> loadMunicipalRanking(forceRefresh = true)
        }
    }

    private fun isCacheStale(): Boolean {
        val oneHourMs = 60 * 60 * 1000
        return System.currentTimeMillis() - lastLoadTime > oneHourMs
    }

    private fun loadGlobalRanking(forceRefresh: Boolean = false) {
        if (!forceRefresh) _uiState.value = RankingUiState.Loading
        viewModelScope.launch {
            val result = rankingCacheRepository.getGlobalRanking(forceRefresh)
            result.onSuccess { data ->
                globalData = data
                lastLoadTime = System.currentTimeMillis()
                updateCacheAge()
                if (_selectedTab.value == RankingTab.GLOBAL) {
                    _uiState.value = RankingUiState.Success(data)
                }
                Log.i(TAG, "Global ranking loaded: myPos=${data.myPosition}")
            }.onFailure { error ->
                if (_selectedTab.value == RankingTab.GLOBAL) {
                    _uiState.value = RankingUiState.Error(error.message ?: "Error cargando ranking")
                }
                Log.e(TAG, "Failed to load global ranking", error)
            }
            _isRefreshing.value = false
        }
    }

    private fun loadMunicipalRanking(forceRefresh: Boolean = false) {
        if (!forceRefresh) _uiState.value = RankingUiState.Loading
        viewModelScope.launch {
            userPreferencesDataStore.cityId.collect { cityId ->
                if (cityId == null) {
                    _uiState.value = RankingUiState.Error("No tienes ciudad configurada")
                    _isRefreshing.value = false
                    return@collect
                }

                val result = rankingCacheRepository.getMunicipalRanking(cityId, forceRefresh)
                result.onSuccess { data ->
                    municipalData = data
                    lastLoadTime = System.currentTimeMillis()
                    updateCacheAge()
                    if (_selectedTab.value == RankingTab.MUNICIPAL) {
                        _uiState.value = RankingUiState.Success(data)
                    }
                    Log.i(TAG, "Municipal ranking loaded: city=$cityId, myPos=${data.myPosition}")
                }.onFailure { error ->
                    if (_selectedTab.value == RankingTab.MUNICIPAL) {
                        _uiState.value = RankingUiState.Error(error.message ?: "Error cargando ranking municipal")
                    }
                    Log.e(TAG, "Failed to load municipal ranking", error)
                }
                _isRefreshing.value = false
            }
        }
    }

    private fun updateCacheAge() {
        val ageMinutes = (System.currentTimeMillis() - lastLoadTime) / (60 * 1000)
        _cacheAgeMinutes.value = ageMinutes
    }

    sealed class RankingUiState {
        data object Loading : RankingUiState()
        data class Success(val data: RankingResponseDto) : RankingUiState()
        data class Error(val message: String) : RankingUiState()
    }

    enum class RankingTab {
        GLOBAL,
        MUNICIPAL
    }
}