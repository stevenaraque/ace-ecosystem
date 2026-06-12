package com.ace.mobile.presentation.exercise

import com.ace.shared.enums.SportType

data class SessionUiState(
    val isSessionActive: Boolean = false,
    val currentHeartRate: Int? = null,
    val elapsedTimeSeconds: Long = 0,
    val sportType: SportType? = null,
    val isConnectedToWear: Boolean = false,
    val xpEarnedThisSession: Int = 0,
    val errorMessage: String? = null
)