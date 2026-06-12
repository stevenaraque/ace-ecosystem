package com.ace.mobile.domain.usecase.auth

import com.ace.mobile.data.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}