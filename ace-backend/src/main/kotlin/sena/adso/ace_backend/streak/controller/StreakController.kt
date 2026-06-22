package sena.adso.ace_backend.streak.controller

import com.ace.shared.dto.StreakStateDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.streak.service.StreakEvaluationService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/streak")
class StreakController(
    private val streakEvaluationService: StreakEvaluationService
) {

    @GetMapping
    fun getCurrentStreak(): ResponseEntity<StreakStateDto> {
        val userId = getCurrentUserId()
        logger.info { "GET /api/streak for user $userId" }

        val streak = streakEvaluationService.getCurrentStreak(userId)

        val response = if (streak != null) {
            StreakStateDto(
                currentStreak = streak.currentStreak,
                bestStreak = streak.bestStreak,
                lastExerciseDate = streak.lastExerciseDate?.toString()
            )
        } else {
            StreakStateDto(
                currentStreak = 0,
                bestStreak = 0,
                lastExerciseDate = null
            )
        }

        return ResponseEntity.ok(response)
    }

    private fun getCurrentUserId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal
            ?: throw IllegalStateException("No authenticated user")
        return when (principal) {
            is String -> UUID.fromString(principal)
            is UUID -> principal
            else -> throw IllegalStateException("Unexpected principal type: ${principal::class.simpleName}")
        }
    }
}