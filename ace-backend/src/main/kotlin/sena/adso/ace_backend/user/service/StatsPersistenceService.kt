package sena.adso.ace_backend.user.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.user.model.UserStats
import sena.adso.ace_backend.user.repository.UserStatsRepository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class StatsPersistenceService(
    private val userStatsRepository: UserStatsRepository
) {

    @Transactional
    fun accumulate(
        userId: UUID,
        xpDelta: Long,
        sessionsDelta: Int,
        blocksDelta: Int,
        durationDelta: Long,
        avgBpm: Double?
    ) {
        val stats = userStatsRepository.findByUserId(userId)
            ?: UserStats(userId = userId)

        stats.totalXp += xpDelta
        stats.totalSessions += sessionsDelta
        stats.totalBlocks += blocksDelta
        stats.totalDurationSeconds += durationDelta

        if (avgBpm != null && stats.totalBlocks > 0) {
            val oldWeight = (stats.totalBlocks - blocksDelta).toDouble()
            val newWeight = blocksDelta.toDouble()
            val currentAvg = stats.avgBpmAllTime ?: 0.0
            stats.avgBpmAllTime = (currentAvg * oldWeight + avgBpm * newWeight) / stats.totalBlocks
        }

        stats.updatedAt = Instant.now()

        userStatsRepository.save(stats)
        logger.info {
            "Stats updated for user $userId: " +
            "xp=${stats.totalXp}, sessions=${stats.totalSessions}, " +
            "blocks=${stats.totalBlocks}"
        }
    }

    @Transactional
    fun overwrite(
        userId: UUID,
        totalXp: Long,
        totalSessions: Int,
        totalBlocks: Int,
        totalDurationSeconds: Long,
        avgBpmAllTime: Double
    ) {
        val stats = userStatsRepository.findByUserId(userId)
            ?: UserStats(userId = userId)

        stats.totalXp = totalXp
        stats.totalSessions = totalSessions
        stats.totalBlocks = totalBlocks
        stats.totalDurationSeconds = totalDurationSeconds
        stats.avgBpmAllTime = avgBpmAllTime
        stats.updatedAt = Instant.now()

        userStatsRepository.save(stats)
        logger.info { "Stats overwritten for user $userId: xp=$totalXp, sessions=$totalSessions" }
    }
}