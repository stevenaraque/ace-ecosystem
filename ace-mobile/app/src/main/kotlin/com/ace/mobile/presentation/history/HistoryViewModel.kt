package com.ace.mobile.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.domain.usecase.history.GetSessionHistoryUseCase
import com.ace.shared.dto.SessionHistoryEntryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getSessionHistoryUseCase: GetSessionHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getSessionHistoryUseCase(forceRefresh = forceRefresh).collect { result ->
                result.onSuccess { history ->
                    _uiState.value = _uiState.value.copy(
                        history = history,
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

    data class HistoryUiState(
        val history: List<SessionHistoryEntryDto> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
}