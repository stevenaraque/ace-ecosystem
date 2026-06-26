package sena.adso.ace_backend.ranking.controller

import com.ace.shared.dto.RankingResponseDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.ranking.service.RankingQueryService
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * FIXME-MVP-HACK: Endpoints de ranking son públicos (permitAll) y on-demand.
 * El recálculo batch y las tablas materializadas (ranking_global / ranking_municipal)
 * quedan obsoletos para este MVP. Ver RankingRecalculationJob.kt @Deprecated.
 */
@RestController
@RequestMapping("/api/ranking")
class RankingController(
    private val rankingQueryService: RankingQueryService
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