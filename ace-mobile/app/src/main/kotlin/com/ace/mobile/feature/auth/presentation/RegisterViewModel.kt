package com.ace.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.core.data.DeviceIdManager
import com.ace.mobile.feature.auth.data.AuthRepository
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdManager: DeviceIdManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RegisterEvent>()
    val event: SharedFlow<RegisterEvent> = _event.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun register() {
        val currentState = _uiState.value

        // Validaciones UI
        when {
            currentState.email.isBlank() -> {
                _uiState.value = currentState.copy(error = "El correo es obligatorio")
                return
            }
            !isValidEmail(currentState.email) -> {
                _uiState.value = currentState.copy(error = "Formato de correo inválido")
                return
            }
            currentState.password.isBlank() -> {
                _uiState.value = currentState.copy(error = "La contraseña es obligatoria")
                return
            }
            currentState.password.length < 8 -> {
                _uiState.value = currentState.copy(error = "La contraseña debe tener al menos 8 caracteres")
                return
            }
            currentState.password != currentState.confirmPassword -> {
                _uiState.value = currentState.copy(error = "Las contraseñas no coinciden")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            val result = authRepository.register(
                email = currentState.email,
                password = currentState.password,
                deviceId = deviceIdManager.deviceId
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isRegistered = true)
                _event.emit(RegisterEvent.NavigateToHome)
            } else {
                val errorMessage = when (val exception = result.exceptionOrNull()) {
                    is retrofit2.HttpException -> {
                        when (exception.code()) {
                            409 -> "Este correo ya está registrado"
                            400 -> "Datos inválidos"
                            else -> "Error del servidor (${exception.code()})"
                        }
                    }
                    else -> exception?.message ?: "Error de conexión"
                }
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val error: String? = null
)

sealed interface RegisterEvent {
    data object NavigateToHome : RegisterEvent
}