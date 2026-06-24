package sena.adso.ace_backend.user.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.user.model.UserStats
import sena.adso.ace_backend.user.repository.UserStatsRepository
import sena.adso.ace_backend.xp.service.XpTransactionService
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class StatsRecalculationService(
    private val userStatsRepository: UserStatsRepository,
    private val xpTransactionService: XpTransactionService,
    @PersistenceContext private val entityManager: EntityManager
) {

    @Transactional
    fun recalculateForUser(userId: UUID) {
        logger.info { "Recalculating stats for user $userId from historical data" }

        val totalXp = xpTransactionService.getCurrentBalance(userId).toLong()
        val totalSessions = countSessions(userId)
        val blockStats = calculateBlockStats(userId)

        val stats = userStatsRepository.findByUserId(userId)
            ?: UserStats(userId = userId)

        stats.totalXp = totalXp
        stats.totalSessions = totalSessions
        stats.totalBlocks = blockStats.totalBlocks
        stats.totalDurationSeconds = blockStats.totalDurationSeconds
        stats.avgBpmAllTime = blockStats.avgBpm
        stats.updatedAt = Instant.now()

        userStatsRepository.save(stats)

        logger.info {
            "Stats recalculated for user $userId: " +
            "xp=$totalXp, sessions=$totalSessions, " +
            "blocks=${blockStats.totalBlocks}, duration=${blockStats.totalDurationSeconds}, " +
            "avgBpm=${blockStats.avgBpm}"
        }
    }

    private fun countSessions(userId: UUID): Int {
        val query = entityManager.createQuery(
            "SELECT COUNT(s) FROM ExerciseSession s WHERE s.userId = :userId",
            Long::class.java
        )
        query.setParameter("userId", userId)
        return (query.singleResult ?: 0L).toInt()
    }

    private data class BlockStats(
        val totalBlocks: Int,
        val totalDurationSeconds: Long,
        val avgBpm: Double?
    )

    private fun calculateBlockStats(userId: UUID): BlockStats {
        val query = entityManager.createQuery(
            "SELECT COUNT(b), SUM(b.durationSeconds), AVG(b.avgBpm) " +
            "FROM ExerciseBlock b WHERE b.userId = :userId",
            Array<Any>::class.java
        )
        query.setParameter("userId", userId)

        val result = query.resultList.firstOrNull()
            ?: return BlockStats(0, 0L, null)

        @Suppress("UNCHECKED_CAST")
        val row = result as Array<Any?>

        val count = (row[0] as? Number)?.toInt() ?: 0
        val duration = (row[1] as? Number)?.toLong() ?: 0L
        val avg = (row[2] as? Number)?.toDouble()

        return BlockStats(count, duration, avg)
    }
}