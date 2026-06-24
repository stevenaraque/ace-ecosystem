package sena.adso.ace_backend.stats.controller

import com.ace.shared.dto.StatsReconcileRequestDto
import com.ace.shared.dto.StatsReconcileResponseDto
import com.ace.shared.dto.StatsResponseDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.stats.service.StatsQueryService
import sena.adso.ace_backend.user.service.StatsConsistencyValidator
import sena.adso.ace_backend.user.service.StatsRecalculationService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/stats")
class StatsController(
    private val statsQueryService: StatsQueryService,
    private val statsConsistencyValidator: StatsConsistencyValidator,
    private val statsRecalculationService: StatsRecalculationService
) {

    @GetMapping
    fun getStats(): ResponseEntity<StatsResponseDto> {
        val userId = getCurrentUserId()
        logger.info { "GET /api/stats for user $userId" }
        return ResponseEntity.ok(statsQueryService.getOfficialStats(userId))
    }

    @PostMapping("/reconcile")
    fun reconcile(
        @RequestBody request: StatsReconcileRequestDto
    ): ResponseEntity<StatsReconcileResponseDto> {
        val userId = getCurrentUserId()
        logger.info { "POST /api/stats/reconcile for user $userId" }

        val validation = statsConsistencyValidator.validateAndCorrect(userId, request.clientStats)

        return ResponseEntity.ok(
            StatsReconcileResponseDto(
                officialStats = validation.officialStats,
                currentRank = validation.currentRank,
                discrepancies = validation.discrepancies
            )
        )
    }

    @PostMapping("/recalculate")
    fun recalculate(): ResponseEntity<Map<String, String>> {
        val userId = getCurrentUserId()
        logger.info { "POST /api/stats/recalculate for user $userId" }

        statsRecalculationService.recalculateForUser(userId)

        return ResponseEntity.ok(
            mapOf(
                "status" to "success",
                "message" to "Stats recalculated from historical data"
            )
        )
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