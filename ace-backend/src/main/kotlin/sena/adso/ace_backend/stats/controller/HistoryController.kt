package sena.adso.ace_backend.history.controller

import com.ace.shared.dto.SessionHistoryEntryDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.history.service.HistoryQueryService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/history")
class HistoryController(
    private val historyQueryService: HistoryQueryService
) {

    @GetMapping
    fun getHistory(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<SessionHistoryEntryDto>> {
        val userId = getCurrentUserId()
        logger.info { "GET /api/history?limit=$limit for user $userId" }

        val history = historyQueryService.getSessionHistory(userId, limit.coerceIn(1, 100))
        return ResponseEntity.ok(history)
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