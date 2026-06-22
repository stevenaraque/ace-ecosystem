package sena.adso.ace_backend.ranking.controller

import com.ace.shared.dto.RankingResponseDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.ranking.service.RankingQueryService
import sena.adso.ace_backend.ranking.service.RankingRecalculationJob
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/ranking")
class RankingController(
    private val rankingQueryService: RankingQueryService,
    private val rankingRecalculationJob: RankingRecalculationJob
) {

    @GetMapping("/global")
    fun getGlobalRanking(): ResponseEntity<RankingResponseDto> {
        val userId = getCurrentUserId()
        logger.info { "GET /api/ranking/global for user $userId" }
        return ResponseEntity.ok(rankingQueryService.getGlobalRanking(userId))
    }

    @GetMapping("/municipal")
    fun getMunicipalRanking(
        @RequestParam cityId: String
    ): ResponseEntity<RankingResponseDto> {
        val userId = getCurrentUserId()
        logger.info { "GET /api/ranking/municipal?cityId=$cityId for user $userId" }
        return ResponseEntity.ok(rankingQueryService.getMunicipalRanking(userId, cityId))
    }

    @PostMapping("/recalculate")
    fun forceRecalculation(): ResponseEntity<Map<String, String>> {
        val userId = getCurrentUserId()
        logger.info { "POST /api/ranking/recalculate triggered by user $userId" }

        val start = System.currentTimeMillis()
        rankingRecalculationJob.recalculateRankings()
        val elapsed = System.currentTimeMillis() - start

        logger.info { "Manual recalculation completed in ${elapsed}ms" }

        return ResponseEntity.ok(
            mapOf(
                "status" to "success",
                "message" to "Ranking recalculated successfully",
                "elapsedMs" to elapsed.toString()
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