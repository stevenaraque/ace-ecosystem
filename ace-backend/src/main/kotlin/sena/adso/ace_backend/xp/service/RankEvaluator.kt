package sena.adso.ace_backend.xp.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import sena.adso.ace_backend.xp.repository.RankCatalogRepository
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RankEvaluator(
    private val rankCatalogRepository: RankCatalogRepository
) {

    fun evaluateRank(userId: UUID, totalXp: Long): RankResult {
        val ranks = rankCatalogRepository.findAllByOrderByMinXpAsc()
        val currentRank = ranks.lastOrNull { it.minXp <= totalXp }
            ?: ranks.first()

        return RankResult(
            rankId = currentRank.id,
            rankName = currentRank.rankName,
            minXp = currentRank.minXp,
            maxXp = currentRank.maxXp
        )
    }

    /**
     * Retorna el siguiente rango al que puede aspirar el usuario.
     * Null si ya está en el rango más alto.
     */
    fun getNextRank(totalXp: Long): RankResult? {
        val ranks = rankCatalogRepository.findAllByOrderByMinXpAsc()
        return ranks.firstOrNull { it.minXp > totalXp }?.let {
            RankResult(
                rankId = it.id,
                rankName = it.rankName,
                minXp = it.minXp,
                maxXp = it.maxXp
            )
        }
    }

    data class RankResult(
        val rankId: java.util.UUID,
        val rankName: String,
        val minXp: Int,
        val maxXp: Int?
    )
}