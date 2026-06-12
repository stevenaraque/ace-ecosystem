package com.ace.mobile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    // CORREGIDO: El tipo de dato debe ser StateFlow
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Flujo de eventos de una sola vía (One-shot events) para la navegación
    private val _event = MutableSharedFlow<ProfileEvent>()
    val event: SharedFlow<ProfileEvent> = _event.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            logoutUseCase().fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.Idle
                    _event.emit(ProfileEvent.NavigateToLogin)
                },
                onFailure = { error ->
                    // Aunque falle la red, el repositorio ya limpió los tokens locales
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error al cerrar sesión")
                    // Forzamos la salida de todos modos porque el estado local ya es inválido
                    _event.emit(ProfileEvent.NavigateToLogin)
                }
            )
        }
    }
}

// Estados de la pantalla de perfil (Mantenerlos fuera de la clase)
sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

// Eventos de navegación (Mantenerlos fuera de la clase)
sealed interface ProfileEvent {
    object NavigateToLogin : ProfileEvent
}