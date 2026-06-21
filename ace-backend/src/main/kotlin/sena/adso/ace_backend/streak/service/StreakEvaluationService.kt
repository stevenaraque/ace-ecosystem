package sena.adso.ace_backend.streak.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.streak.model.UserStreak
import sena.adso.ace_backend.streak.repository.UserStreakRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class StreakEvaluationService(
    private val userStreakRepository: UserStreakRepository
) {

    /**
     * Evalúa la racha del usuario cada vez que un bloque es validado.
     * Se ejecuta dentro de la MISMA transacción que inserta el bloque y la XP.
     * 
     * Reglas (Apéndice S7 §2.3):
     * - Mismo día: no cambia
     * - Día siguiente: current_streak += 1, actualiza best si aplica
     * - Hueco > 1 día: current_streak = 1 (se rompe)
     * - Primera vez: current_streak = 1, best_streak = 1
     */
    @Transactional
    fun evaluateStreak(userId: UUID, blockTimestampStart: Instant): UserStreak {
        
        val blockDate = blockTimestampStart.atZone(ZoneId.systemDefault()).toLocalDate()
        val userStreak = userStreakRepository.findById(userId)
            .orElse(null) ?: createInitialStreak(userId)

        val lastDate = userStreak.lastExerciseDate

        val updatedStreak = when {
            // Primera vez
            lastDate == null -> userStreak.copy(
                currentStreak = 1,
                bestStreak = 1,
                lastExerciseDate = blockDate
            )
            
            // Mismo día: no cambia
            lastDate == blockDate -> userStreak
            
            // Día siguiente: incrementa
            lastDate.plusDays(1) == blockDate -> {
                val newCurrent = userStreak.currentStreak + 1
                userStreak.copy(
                    currentStreak = newCurrent,
                    bestStreak = maxOf(newCurrent, userStreak.bestStreak),
                    lastExerciseDate = blockDate
                )
            }
            
            // Hueco > 1 día: se rompe
            else -> userStreak.copy(
                currentStreak = 1,
                lastExerciseDate = blockDate
            )
        }

        val saved = userStreakRepository.save(updatedStreak)
        logger.info { 
            "Streak evaluated for user $userId: " +
            "current=${saved.currentStreak}, best=${saved.bestStreak}, " +
            "lastDate=${saved.lastExerciseDate}" 
        }
        
        return saved
    }

    fun getCurrentStreak(userId: UUID): UserStreak? {
    return userStreakRepository.findById(userId).orElse(null)
}

    private fun createInitialStreak(userId: UUID): UserStreak {
        return UserStreak(
            userId = userId,
            currentStreak = 0,
            bestStreak = 0,
            lastExerciseDate = null
        )
    }
}