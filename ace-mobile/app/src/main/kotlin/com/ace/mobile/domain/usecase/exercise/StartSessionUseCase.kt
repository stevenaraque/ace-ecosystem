package com.ace.mobile.domain.usecase.exercise

import com.ace.mobile.data.local.entity.LocalSessionEntity
import com.ace.mobile.data.repository.SessionRepository
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.service.ExerciseSyncServiceManager
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseSyncServiceManager: ExerciseSyncServiceManager
) {
    sealed class Result {
        data class Success(val session: ExerciseSession) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(sportType: SportType): Result = withContext(Dispatchers.IO) {
        try {
            val activeSession = sessionRepository.getActiveSession()
            if (activeSession != null) {
                sessionRepository.abortSession(activeSession.sessionId)
            }

            val session = ExerciseSession.create(sportType)

            val entity = LocalSessionEntity(
                sessionId = session.sessionId,
                status = SessionStatus.ACTIVE,
                sportType = sportType,
                timestampStart = session.timestampStart
            )
            sessionRepository.insertSession(entity)

            exerciseSyncServiceManager.startService(session.sessionId, sportType)

            Result.Success(session)

        } catch (e: Exception) {
            Result.Error("Error al iniciar sesión: ${e.message}")
        }
    }
}