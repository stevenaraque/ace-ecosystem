package sena.adso.ace_backend.ranking.service

import com.ace.shared.constants.RankingConstants
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.ranking.model.RankingGlobal
import sena.adso.ace_backend.ranking.model.RankingMunicipal
import sena.adso.ace_backend.ranking.repository.RankingGlobalRepository
import sena.adso.ace_backend.ranking.repository.RankingMunicipalRepository
import sena.adso.ace_backend.user.model.UserProfile
import sena.adso.ace_backend.user.repository.UserProfileRepository
import sena.adso.ace_backend.xp.repository.XpTransactionRepository
import sena.adso.ace_backend.xp.service.RankEvaluator
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * FIXME-MVP-OBSOLETO: Este job batch queda deshabilitado para el MVP.
 * El ranking ahora se calcula on-demand en RankingQueryService via SQL nativo.
 * 
 * Conservado por si en el futuro se vuelve a tablas materializadas
 * (por ejemplo, con >10,000 usuarios).
 */
@Deprecated(
    message = "Reemplazado por ranking on-demand en RankingQueryService. No usar en MVP.",
    replaceWith = ReplaceWith("RankingQueryService")
)
@Component
class RankingRecalculationJob(
    private val xpTransactionRepository: XpTransactionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val rankEvaluator: RankEvaluator,
    private val rankingGlobalRepository: RankingGlobalRepository,
    private val rankingMunicipalRepository: RankingMunicipalRepository,
    private val municipalFilterService: MunicipalFilterService
) {

    /**
     * FIXME-MVP: Comentado para evitar que corra en Render free tier.
     * El spin-down de Render mata la JVM y el job nunca se dispara.
     * Si se reactiva, requiere plan pago o Cron Job separado.
     */
    // @Scheduled(cron = RankingConstants.RANKING_RECALCULATION_CRON)
    @Transactional
    fun recalculateRankings() {
        logger.info { "=== START RankingRecalculationJob (OBSOLETO) at ${LocalDateTime.now()} ===" }

        try {
            val xpByUser = xpTransactionRepository.findAll()
                .groupBy { it.userId }
                .mapValues { entry -> entry.value.sumOf { it.xpAmount.toLong() } }

            if (xpByUser.isEmpty()) {
                logger.warn { "No XP transactions. Skipping." }
                return
            }

            logger.info { "Aggregated XP for ${xpByUser.size} users" }

            rankingGlobalRepository.deleteAllEntries()
            rankingMunicipalRepository.deleteAllEntries()

            val profilesById = userProfileRepository.findAll().associateBy { it.userId }

            val globalEntries = calculateGlobalRanking(xpByUser, profilesById)
            rankingGlobalRepository.saveAll(globalEntries)
            logger.info { "Global: ${globalEntries.size} entries" }

            val municipalEntries = calculateMunicipalRanking(xpByUser, profilesById)
            rankingMunicipalRepository.saveAll(municipalEntries)
            logger.info { "Municipal: ${municipalEntries.size} entries" }

        } catch (ex: Exception) {
            logger.error(ex) { "Ranking recalculation failed" }
            throw ex
        }

        logger.info { "=== END RankingRecalculationJob at ${LocalDateTime.now()} ===" }
    }

    private fun calculateGlobalRanking(
        xpByUser: Map<UUID, Long>,
        profiles: Map<UUID, UserProfile>
    ): List<RankingGlobal> {
        return xpByUser.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (userId, totalXp) ->
                val profile = profiles[userId]
                val rankResult = rankEvaluator.evaluateRank(userId, totalXp)

                RankingGlobal(
                    userId = userId,
                    username = municipalFilterService.getDisplayName(profile ?: UserProfile(userId = userId)),
                    totalXp = totalXp,
                    rankName = rankResult.rankName,
                    position = index + 1,
                    updatedAt = LocalDateTime.now()
                )
            }
    }

    private fun calculateMunicipalRanking(
        xpByUser: Map<UUID, Long>,
        profiles: Map<UUID, UserProfile>
    ): List<RankingMunicipal> {
        val withCity = xpByUser.filter { (userId, _) ->
            !profiles[userId]?.cityId.isNullOrBlank()
        }

        return withCity.entries
            .groupBy { profiles[it.key]?.cityId!! }
            .flatMap { (_, userXpList) ->
                userXpList
                    .sortedByDescending { it.value }
                    .mapIndexed { index, (userId, totalXp) ->
                        val profile = profiles[userId]!!
                        val rankResult = rankEvaluator.evaluateRank(userId, totalXp)

                        RankingMunicipal(
                            userId = userId,
                            username = municipalFilterService.getDisplayName(profile),
                            totalXp = totalXp,
                            rankName = rankResult.rankName,
                            position = index + 1,
                            cityId = profile.cityId!!,
                            updatedAt = LocalDateTime.now()
                        )
                    }
            }
    }
}