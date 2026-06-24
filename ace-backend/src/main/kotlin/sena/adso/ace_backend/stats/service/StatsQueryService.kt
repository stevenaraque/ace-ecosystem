package sena.adso.ace_backend.stats.service

import com.ace.shared.dto.StatsResponseDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.user.model.UserStats
import sena.adso.ace_backend.user.repository.UserStatsRepository
import sena.adso.ace_backend.xp.service.RankEvaluator
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class StatsQueryService(
    private val userStatsRepository: UserStatsRepository,
    private val rankEvaluator: RankEvaluator
) {

    @Transactional(readOnly = true)
    fun getOfficialStats(userId: UUID): StatsResponseDto {
        val stats = userStatsRepository.findByUserId(userId)
            ?: UserStats(userId = userId)

        val rankResult = rankEvaluator.evaluateRank(userId, stats.totalXp)
        val nextRank = rankEvaluator.getNextRank(stats.totalXp)

        logger.info {
            "Stats for user $userId: xp=${stats.totalXp}, " +
            "sessions=${stats.totalSessions}, rank=${rankResult.rankName}"
        }

        return StatsResponseDto(
            totalXp = stats.totalXp,
            totalSessions = stats.totalSessions,
            totalBlocks = stats.totalBlocks,
            totalDurationSeconds = stats.totalDurationSeconds,
            avgBpmAllTime = stats.avgBpmAllTime ?: 0.0,
            currentRank = rankResult.rankName,
            nextRank = nextRank?.rankName,
            xpToNextRank = nextRank?.let { (it.minXp.toLong() - stats.totalXp).coerceAtLeast(0) }
        )
    }
}