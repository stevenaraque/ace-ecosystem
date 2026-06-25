package com.ace.mobile.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.feature.auth.domain.LogoutUseCase
import com.ace.mobile.feature.profile.data.UserRepository
import com.ace.shared.dto.UpdateProfileRequestDto
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
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ProfileEvent>()
    val event: SharedFlow<ProfileEvent> = _event.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.getProfile()
            result.onSuccess { profile ->
                _uiState.value = ProfileUiState.Success(
                    profile = ProfileData(
                        userId = profile.userId,
                        username = profile.username,
                        nickname = profile.nickname,
                        cityId = profile.cityId,
                        weightKg = profile.weightKg,
                        birthDate = profile.birthDate
                    ),
                    isEditing = false
                )
            }.onFailure { error ->
                // Fallback: si falla la red, mostrar al menos los datos que tengamos
                _uiState.value = ProfileUiState.Error(
                    message = error.message ?: "Error cargando perfil",
                    canRetry = true
                )
            }
        }
    }

    fun startEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(isEditing = true)
        }
    }

    fun cancelEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(isEditing = false)
        }
    }

    fun updateField(field: ProfileField, value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            val updatedProfile = when (field) {
                ProfileField.USERNAME -> current.profile.copy(username = value)
                ProfileField.NICKNAME -> current.profile.copy(nickname = value)
                ProfileField.CITY_ID -> current.profile.copy(cityId = value)
                ProfileField.WEIGHT_KG -> current.profile.copy(weightKg = value.toDoubleOrNull())
                ProfileField.BIRTH_DATE -> current.profile.copy(birthDate = value)
            }
            _uiState.value = current.copy(profile = updatedProfile)
        }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current !is ProfileUiState.Success) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)

            val request = UpdateProfileRequestDto(
                username = current.profile.username,
                nickname = current.profile.nickname,
                cityId = current.profile.cityId,
                weightKg = current.profile.weightKg,
                birthDate = current.profile.birthDate
            )

            val result = userRepository.updateProfile(request)
            result.onSuccess { updatedProfile ->
                _uiState.value = ProfileUiState.Success(
                    profile = ProfileData(
                        userId = updatedProfile.userId,
                        username = updatedProfile.username,
                        nickname = updatedProfile.nickname,
                        cityId = updatedProfile.cityId,
                        weightKg = updatedProfile.weightKg,
                        birthDate = updatedProfile.birthDate
                    ),
                    isEditing = false,
                    isSaving = false,
                    saveSuccess = true
                )
                // El cityId ya se persistió en DataStore dentro de UserRepository
            }.onFailure { error ->
                _uiState.value = current.copy(
                    isSaving = false,
                    saveError = error.message ?: "Error guardando perfil"
                )
            }
        }
    }

    fun clearSaveSuccess() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(saveSuccess = false)
        }
    }

    fun clearSaveError() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(saveError = null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            logoutUseCase().fold(
                onSuccess = {
                    _event.emit(ProfileEvent.NavigateToLogin)
                },
                onFailure = { error ->
                    // Aunque falle la red, el repositorio ya limpió los tokens locales
                    _event.emit(ProfileEvent.NavigateToLogin)
                }
            )
        }
    }
}

// ─── Estados ─────────────────────────────────────────────────────────────────

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val profile: ProfileData,
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val saveError: String? = null
    ) : ProfileUiState
    data class Error(val message: String, val canRetry: Boolean = true) : ProfileUiState
}

data class ProfileData(
    val userId: String = "",
    val username: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val cityId: String? = null,
    val weightKg: Double? = null,
    val birthDate: String? = null
)

enum class ProfileField {
    USERNAME,
    NICKNAME,
    CITY_ID,
    WEIGHT_KG,
    BIRTH_DATE
}

// ─── Eventos ─────────────────────────────────────────────────────────────────

sealed interface ProfileEvent {
    data object NavigateToLogin : ProfileEvent
}