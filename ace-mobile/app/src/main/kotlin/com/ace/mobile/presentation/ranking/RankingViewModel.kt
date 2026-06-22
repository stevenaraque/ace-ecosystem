package com.ace.mobile.presentation.ranking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.data.local.datastore.UserPreferencesDataStore
import com.ace.mobile.data.repository.RankingCacheRepository
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var globalData: RankingResponseDto? = null
    private var municipalData: RankingResponseDto? = null

    init {
        loadGlobalRanking()
    }

    fun selectTab(tab: RankingTab) {
        _selectedTab.value = tab
        when (tab) {
            RankingTab.GLOBAL -> {
                if (globalData != null) {
                    _uiState.value = RankingUiState.Success(globalData!!)
                } else {
                    loadGlobalRanking()
                }
            }
            RankingTab.MUNICIPAL -> {
                if (municipalData != null) {
                    _uiState.value = RankingUiState.Success(municipalData!!)
                } else {
                    loadMunicipalRanking()
                }
            }
        }
    }

    fun refresh() {
        when (_selectedTab.value) {
            RankingTab.GLOBAL -> loadGlobalRanking(forceRefresh = true)
            RankingTab.MUNICIPAL -> loadMunicipalRanking(forceRefresh = true)
        }
    }

    private fun loadGlobalRanking(forceRefresh: Boolean = false) {
        _uiState.value = RankingUiState.Loading
        viewModelScope.launch {
            val result = rankingCacheRepository.getGlobalRanking(forceRefresh)
            result.onSuccess { data ->
                globalData = data
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
        }
    }

    private fun loadMunicipalRanking(forceRefresh: Boolean = false) {
        _uiState.value = RankingUiState.Loading
        viewModelScope.launch {
            userPreferencesDataStore.cityId.collect { cityId ->
                if (cityId == null) {
                    _uiState.value = RankingUiState.Error("No tienes ciudad configurada")
                    return@collect
                }

                val result = rankingCacheRepository.getMunicipalRanking(cityId, forceRefresh)
                result.onSuccess { data ->
                    municipalData = data
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
            }
        }
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