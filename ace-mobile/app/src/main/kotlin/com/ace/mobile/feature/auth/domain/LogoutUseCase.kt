package com.ace.mobile.feature.auth.domain

import com.ace.mobile.feature.auth.data.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}