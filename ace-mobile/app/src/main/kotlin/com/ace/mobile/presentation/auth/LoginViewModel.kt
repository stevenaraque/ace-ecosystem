package com.ace.mobile.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.data.local.DeviceIdManager
import com.ace.mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdManager: DeviceIdManager
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(email: String) {
        uiState = uiState.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password)
    }

    fun login() {
        if (uiState.email.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Email y contraseña son obligatorios")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val result = authRepository.login(
                email = uiState.email,
                password = uiState.password,
                deviceId = deviceIdManager.deviceId
            )
            uiState = if (result.isSuccess) {
                uiState.copy(isLoading = false, isLoggedIn = true)
            } else {
                uiState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Error de autenticación"
                )
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)