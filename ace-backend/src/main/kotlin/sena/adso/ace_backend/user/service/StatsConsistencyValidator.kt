package sena.adso.ace_backend.user.service

import com.ace.shared.dto.ClientStatsDto
import com.ace.shared.dto.OfficialStatsDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.user.model.UserStats
import sena.adso.ace_backend.user.repository.UserStatsRepository
import sena.adso.ace_backend.xp.service.RankEvaluator
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class StatsConsistencyValidator(
    private val userStatsRepository: UserStatsRepository,
    private val rankEvaluator: RankEvaluator
) {

    data class ValidationResult(
        val officialStats: OfficialStatsDto,
        val currentRank: String,
        val discrepancies: List<String>
    )

    @Transactional(readOnly = true)
    fun validateAndCorrect(userId: UUID, clientStats: ClientStatsDto): ValidationResult {
        val official = userStatsRepository.findByUserId(userId)
            ?: UserStats(userId = userId)

        val discrepancies = mutableListOf<String>()

        if (clientStats.totalXp != official.totalXp) {
            discrepancies.add("XP: client=${clientStats.totalXp}, official=${official.totalXp}")
        }
        if (clientStats.totalSessions != official.totalSessions) {
            discrepancies.add("Sessions: client=${clientStats.totalSessions}, official=${official.totalSessions}")
        }
        if (clientStats.totalBlocks != official.totalBlocks) {
            discrepancies.add("Blocks: client=${clientStats.totalBlocks}, official=${official.totalBlocks}")
        }
        if (clientStats.totalDurationSeconds != official.totalDurationSeconds) {
            discrepancies.add("Duration: client=${clientStats.totalDurationSeconds}, official=${official.totalDurationSeconds}")
        }

        val correctionApplied = discrepancies.isNotEmpty()
        if (correctionApplied) {
            logger.warn { "Stats discrepancies for user $userId: $discrepancies" }
        } else {
            logger.debug { "Stats consistent for user $userId" }
        }

        val rankResult = rankEvaluator.evaluateRank(userId, official.totalXp)

        return ValidationResult(
            officialStats = OfficialStatsDto(
                officialTotalXp = official.totalXp,
                officialTotalSessions = official.totalSessions,
                officialTotalBlocks = official.totalBlocks,
                officialTotalDurationSeconds = official.totalDurationSeconds,
                officialAvgBpmAllTime = official.avgBpmAllTime ?: 0.0,
                correctionApplied = correctionApplied,
                correctionReason = if (correctionApplied) discrepancies.joinToString("; ") else null
            ),
            currentRank = rankResult.rankName,
            discrepancies = discrepancies
        )
    }
}