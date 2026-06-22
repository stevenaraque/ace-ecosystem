package sena.adso.ace_backend.xp.controller

import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sena.adso.ace_backend.xp.dto.RankInfoResponse
import sena.adso.ace_backend.xp.service.RankEvaluator
import sena.adso.ace_backend.xp.service.XpTransactionService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/xp")
class RankController(
    private val rankEvaluator: RankEvaluator,
    private val xpTransactionService: XpTransactionService
) {

    @GetMapping("/rank")
    fun getCurrentRank(): ResponseEntity<RankInfoResponse> {
        // Leer userId del SecurityContext (seteado por JwtAuthenticationFilter)
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
            ?: return ResponseEntity.status(401).build()
        
        val userId = UUID.fromString(authentication.name)
        val totalXp = xpTransactionService.getCurrentBalance(userId).toLong()
        
        val rank = rankEvaluator.evaluateRank(userId, totalXp)
        val nextRank = rankEvaluator.getNextRank(totalXp)
        val xpToNext = nextRank?.let { it.minXp - totalXp }?.toInt()

        logger.debug { "Rank query for user $userId: ${rank.rankName}, xp=$totalXp" }

        return ResponseEntity.ok(
            RankInfoResponse(
                currentRank = rank.rankName,
                nextRank = nextRank?.rankName,
                xpToNextRank = xpToNext,
                totalXp = totalXp
            )
        )
    }
}