package sena.adso.ace_backend.exercise.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.exercise.model.ExerciseSession
import java.util.UUID

@Repository
interface SessionRepository : JpaRepository<ExerciseSession, UUID> {
    fun existsBySessionId(sessionId: UUID): Boolean
    fun findByUserIdAndStatus(userId: UUID, status: String): ExerciseSession?

    fun findByUserIdOrderByTimestampStartDesc(userId: UUID, pageable: Pageable): List<ExerciseSession>
}