package sena.adso.ace_backend.exercise.service

import com.ace.shared.constants.SyncConstants
import com.ace.shared.dto.*
import com.ace.shared.enums.SessionStatus
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.exercise.model.ExerciseSession
import sena.adso.ace_backend.exercise.repository.SessionRepository
import sena.adso.ace_backend.streak.service.StreakEvaluationService
import sena.adso.ace_backend.user.service.StatsPersistenceService
import sena.adso.ace_backend.xp.service.RankEvaluator
import sena.adso.ace_backend.xp.service.XpSanityValidator
import sena.adso.ace_backend.xp.service.XpTransactionService
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class SyncBatchService(
    private val blockPersistenceService: BlockPersistenceService,
    private val xpSanityValidator: XpSanityValidator,
    private val xpTransactionService: XpTransactionService,
    private val streakEvaluationService: StreakEvaluationService,
    private val sessionRepository: SessionRepository,
    private val rankEvaluator: RankEvaluator,
    private val statsPersistenceService: StatsPersistenceService
) {

    @Transactional
    fun processBatch(request: SyncBatchRequestDto, userId: UUID): SyncBatchResponseDto {
        logger.info {
            "Processing sync batch: user=$userId, " +
            "session=${request.sessionId}, " +
            "blocks=${request.blocks.size}, " +
            "device=${request.deviceId}, " +
            "sentAt=${request.sentAt}"
        }

        val balanceBefore = xpTransactionService.getCurrentBalance(userId)
        val rankBefore = rankEvaluator.evaluateRank(userId, balanceBefore)
        logger.debug { "User $userId rank before: ${rankBefore.rankName} (xp=$balanceBefore)" }

        persistOrUpdateSession(request, userId)

        val acceptedBlocks = mutableListOf<String>()
        val rejectedBlocks = mutableListOf<RejectedBlockDto>()
        val xpDetails = mutableListOf<XpAwardedResponseDto>()

        for (blockDto in request.blocks) {
            val result = processSingleBlock(blockDto, userId)
            
            when {
                result.isAccepted -> {
                    acceptedBlocks.add(blockDto.blockId)
                    xpDetails.add(result.xpDetail!!)
                }
                else -> {
                    rejectedBlocks.add(
                        RejectedBlockDto(
                            blockId = blockDto.blockId,
                            reason = result.rejectionReason!!,
                            ruleViolated = result.ruleViolated!!
                        )
                    )
                    xpDetails.add(result.xpDetail!!)
                }
            }
        }

        val balanceAfter = xpTransactionService.getCurrentBalance(userId)
        val rankAfter = rankEvaluator.evaluateRank(userId, balanceAfter)
        val rankChanged = rankBefore.rankId != rankAfter.rankId
        
        if (rankChanged) {
            logger.info { "User $userId RANK UP! ${rankBefore.rankName} → ${rankAfter.rankName}" }
        }

        // C5 FIX: usar el último bloque aceptado (más reciente por timestamp), no el primero
        val lastAcceptedBlock = request.blocks
            .filter { it.blockId in acceptedBlocks }
            .maxByOrNull { it.timestampStart }

        val streakState = if (lastAcceptedBlock != null) {
            streakEvaluationService.evaluateStreak(
                userId,
                Instant.ofEpochMilli(lastAcceptedBlock.timestampStart)
            ).let {
                StreakStateDto(
                    currentStreak = it.currentStreak,
                    bestStreak = it.bestStreak,
                    lastExerciseDate = it.lastExerciseDate?.toString()
                )
            }
        } else {
            val currentStreak = streakEvaluationService.getCurrentStreak(userId)
            StreakStateDto(
                currentStreak = currentStreak?.currentStreak ?: 0,
                bestStreak = currentStreak?.bestStreak ?: 0,
                lastExerciseDate = currentStreak?.lastExerciseDate?.toString()
            )
        }

        val acceptedBlockDtos = request.blocks.filter { it.blockId in acceptedBlocks }
        val acceptedDuration = acceptedBlockDtos.sumOf { it.durationSeconds.toLong() }
        val acceptedXp = acceptedBlockDtos.sumOf { it.xpCalculated.toLong() }
        val acceptedAvgBpm = acceptedBlockDtos
            .map { it.avgBpm }
            .average()
            .takeIf { it.isFinite() }

        statsPersistenceService.accumulate(
            userId = userId,
            xpDelta = acceptedXp,
            sessionsDelta = if (acceptedBlockDtos.isNotEmpty()) 1 else 0,
            blocksDelta = acceptedBlockDtos.size,
            durationDelta = acceptedDuration,
            avgBpm = acceptedAvgBpm
        )

        logger.info {
            "Stats persisted for user $userId: " +
            "xpDelta=$acceptedXp, sessionsDelta=${if (acceptedBlockDtos.isNotEmpty()) 1 else 0}, " +
            "blocksDelta=${acceptedBlockDtos.size}, duration=$acceptedDuration"
        }

        val officialStats = OfficialStatsDto(
            officialTotalXp = balanceAfter,
            officialTotalSessions = request.clientStats.totalSessions,
            officialTotalBlocks = acceptedBlocks.size,
            officialTotalDurationSeconds = acceptedDuration,
            officialAvgBpmAllTime = request.clientStats.avgBpmAllTime,
            correctionApplied = rejectedBlocks.isNotEmpty(),
            correctionReason = if (rejectedBlocks.isNotEmpty()) 
                "${rejectedBlocks.size} bloques rechazados" else null
        )

        logger.info {
            "Batch processed for user $userId: " +
            "${acceptedBlocks.size} accepted, " +
            "${rejectedBlocks.size} rejected, " +
            "balance=$balanceAfter, " +
            "rank=${rankAfter.rankName}, " +
            "rankChanged=$rankChanged"
        }

        return SyncBatchResponseDto(
            acceptedBlocks = acceptedBlocks,
            rejectedBlocks = rejectedBlocks,
            officialStats = officialStats,
            streakState = streakState,
            rankChanged = rankChanged,
            xpDetails = xpDetails
        )
    }

    private fun persistOrUpdateSession(request: SyncBatchRequestDto, userId: UUID) {
        if (request.blocks.isEmpty()) return

        val firstBlock = request.blocks.first()
        val sessionId = try {
            UUID.fromString(firstBlock.sessionId)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid sessionId format: ${firstBlock.sessionId}" }
            return
        }

        val acceptedBlocks = request.blocks.filter { it.xpCalculated > 0 }
        val totalXp = acceptedBlocks.sumOf { it.xpCalculated }

        if (!sessionRepository.existsBySessionId(sessionId)) {
            val session = ExerciseSession(
                sessionId = sessionId,
                userId = userId,
                deviceId = firstBlock.deviceId,
                sportType = firstBlock.sportType,
                status = SessionStatus.COMPLETED,
                timestampStart = Instant.ofEpochMilli(firstBlock.timestampStart),
                timestampEnd = Instant.ofEpochMilli(firstBlock.timestampEnd),
                totalBlocks = request.blocks.size,
                totalXp = totalXp,
                schemaVersion = request.schemaVersion
            )
            sessionRepository.save(session)
            logger.info { "Session $sessionId persisted with ${request.blocks.size} blocks, totalXp=$totalXp" }
        } else {
            val existing = sessionRepository.findById(sessionId).orElse(null)
            existing?.let {
                it.totalBlocks += request.blocks.size
                it.totalXp += totalXp
                it.timestampEnd = Instant.ofEpochMilli(
                    maxOf(it.timestampEnd?.toEpochMilli() ?: 0L, firstBlock.timestampEnd)
                )
                sessionRepository.save(it)
                logger.debug { "Session $sessionId updated: blocks=${it.totalBlocks}, xp=${it.totalXp}" }
            }
        }
    }

    private data class BlockResult(
        val isAccepted: Boolean,
        val xpDetail: XpAwardedResponseDto? = null,
        val rejectionReason: String? = null,
        val ruleViolated: String? = null
    )

    private fun processSingleBlock(
        dto: ExerciseBlockDto,
        userId: UUID
    ): BlockResult {
        
        val validationResult = xpSanityValidator.validate(dto)
        
        if (validationResult is XpSanityValidator.ValidationResult.Invalid) {
            logger.warn { "Block ${dto.blockId} rejected: ${validationResult.reason}" }
            
            val currentBalance = xpTransactionService.getCurrentBalance(userId)
            
            return BlockResult(
                isAccepted = false,
                xpDetail = XpAwardedResponseDto(
                    blockId = dto.blockId,
                    xpAccepted = 0,
                    xpRejected = dto.xpCalculated,
                    newTotalXp = currentBalance,
                    rankChanged = false,
                    newRankId = null,
                    balanceAfter = currentBalance,
                    schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
                ),
                rejectionReason = validationResult.reason,
                ruleViolated = "SANITY_CHECK"
            )
        }

        val wasPersisted = blockPersistenceService.persistIfNotExists(dto)
        
        if (!wasPersisted) {
            val currentBalance = xpTransactionService.getCurrentBalance(userId)
            
            return BlockResult(
                isAccepted = true,
                xpDetail = XpAwardedResponseDto(
                    blockId = dto.blockId,
                    xpAccepted = 0,
                    xpRejected = 0,
                    newTotalXp = currentBalance,
                    rankChanged = false,
                    newRankId = null,
                    balanceAfter = currentBalance,
                    schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
                )
            )
        }

        val transaction = xpTransactionService.recordXpTransaction(userId, dto)
        
        return BlockResult(
            isAccepted = true,
            xpDetail = XpAwardedResponseDto(
                blockId = dto.blockId,
                xpAccepted = dto.xpCalculated,
                xpRejected = 0,
                newTotalXp = transaction.balanceAfter,
                rankChanged = false,
                newRankId = null,
                balanceAfter = transaction.balanceAfter,
                schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
            )
        )
    }
}