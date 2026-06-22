package sena.adso.ace_backend.ranking.service

import com.ace.shared.constants.RankingConstants
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.ranking.repository.RankingGlobalRepository
import sena.adso.ace_backend.ranking.repository.RankingMunicipalRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class RankingQueryService(
    private val rankingGlobalRepository: RankingGlobalRepository,
    private val rankingMunicipalRepository: RankingMunicipalRepository
) {

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_DATE_TIME
    }

    @Transactional(readOnly = true)
    fun getGlobalRanking(currentUserId: UUID): RankingResponseDto {
        val all = rankingGlobalRepository.findAllByOrderByPositionAsc()
        val top = all.take(RankingConstants.TOP_GLOBAL_SIZE).map { it.toEntryDto() }

        val myEntry = all.find { it.userId == currentUserId }

        logger.info { "Global ranking for user $currentUserId, top ${top.size}" }

        return RankingResponseDto(
            myPosition = myEntry?.position ?: 0,
            myTotalXp = myEntry?.totalXp ?: 0L,
            top = top,
            lastUpdated = myEntry?.updatedAt?.format(FORMATTER)
                ?: LocalDateTime.now().format(FORMATTER)
        )
    }

    @Transactional(readOnly = true)
    fun getMunicipalRanking(currentUserId: UUID, cityId: String): RankingResponseDto {
        val all = rankingMunicipalRepository.findByCityIdOrderByPositionAsc(cityId)
        val top = all.take(RankingConstants.TOP_GLOBAL_SIZE).map { it.toEntryDto() }

        val myEntry = all.find { it.userId == currentUserId }

        logger.info { "Municipal ranking for user $currentUserId, city=$cityId, top ${top.size}" }

        return RankingResponseDto(
            myPosition = myEntry?.position ?: 0,
            myTotalXp = myEntry?.totalXp ?: 0L,
            top = top,
            lastUpdated = myEntry?.updatedAt?.format(FORMATTER)
                ?: LocalDateTime.now().format(FORMATTER)
        )
    }

    private fun sena.adso.ace_backend.ranking.model.RankingGlobal.toEntryDto() = RankingEntryDto(
        position = position,
        userId = userId.toString(),
        totalXp = totalXp,
        username = username
    )

    private fun sena.adso.ace_backend.ranking.model.RankingMunicipal.toEntryDto() = RankingEntryDto(
        position = position,
        userId = userId.toString(),
        totalXp = totalXp,
        username = username
    )
}