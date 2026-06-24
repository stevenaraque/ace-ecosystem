package sena.adso.ace_backend.history.service

import com.ace.shared.dto.SessionHistoryEntryDto
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.exercise.model.ExerciseSession
import sena.adso.ace_backend.exercise.repository.SessionRepository
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class HistoryQueryService(
    private val sessionRepository: SessionRepository
) {

    companion object {
        private val ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    @Transactional(readOnly = true)
    fun getSessionHistory(userId: UUID, limit: Int): List<SessionHistoryEntryDto> {
        val sessions = sessionRepository.findByUserIdOrderByTimestampStartDesc(userId, PageRequest.of(0, limit))

        logger.info { "History for user $userId: ${sessions.size} sessions returned (limit=$limit)" }

        return sessions.map { it.toHistoryEntryDto() }
    }

    private fun ExerciseSession.toHistoryEntryDto(): SessionHistoryEntryDto {
        val duration = timestampEnd?.let {
            java.time.Duration.between(timestampStart, it).seconds.toInt()
        } ?: 0

        return SessionHistoryEntryDto(
            sessionId = sessionId.toString(),
            timestampStart = timestampStart.atOffset(ZoneOffset.UTC).format(ISO_FORMATTER),
            timestampEnd = timestampEnd?.atOffset(ZoneOffset.UTC)?.format(ISO_FORMATTER),
            sportType = sportType.name,
            durationSeconds = duration,
            totalXp = totalXp,
            totalBlocks = totalBlocks,
            avgBpm = null
        )
    }
}