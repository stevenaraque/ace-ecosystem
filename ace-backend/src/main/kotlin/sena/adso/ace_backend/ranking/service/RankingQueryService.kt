package sena.adso.ace_backend.ranking.service

import com.ace.shared.constants.RankingConstants
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

private val logger = KotlinLogging.logger {}

/**
 * FIXME-MVP-HACK: Ranking calculado on-demand via SQL nativo.
 * Reemplaza el sistema de tablas materializadas (ranking_global / ranking_municipal)
 * y el job batch (RankingRecalculationJob) para este MVP.
 *
 * En refactor post-MVP, migrar a tabla user_xp_balance + índices optimizados.
 */
@Service
class RankingQueryService {

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_DATE_TIME
    }

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    fun getGlobalRanking(currentUserId: UUID): RankingResponseDto {
        val sql = """
            SELECT 
                u.user_id,
                u.username,
                u.nickname,
                COALESCE(SUM(x.xp_amount), 0) as total_xp
            FROM user_profile u
            LEFT JOIN xp_transactions x ON u.user_id = x.user_id
            GROUP BY u.user_id, u.username, u.nickname
            ORDER BY total_xp DESC
        """.trimIndent()

        val results = entityManager.createNativeQuery(sql)
            .resultList as List<Array<*>>

        val entries = results.mapIndexed { index, row ->
            val userId = (row[0] as java.util.UUID)
            val username = (row[1] as? String) ?: ""
            val nickname = (row[2] as? String)
            val totalXp = (row[3] as Number).toLong()

            val displayName = nickname?.takeIf { it.isNotBlank() } 
                ?: username.takeIf { it.isNotBlank() } 
                ?: "Usuario ${userId.toString().take(8)}"

            RankingEntryDto(
                position = index + 1,
                userId = userId.toString(),
                totalXp = totalXp,
                username = displayName
            )
        }

        val top = entries.take(RankingConstants.TOP_GLOBAL_SIZE)
        val myEntry = entries.find { it.userId == currentUserId.toString() }

        logger.info { "Global ranking calculated on-demand: ${entries.size} users, myPos=${myEntry?.position ?: 0}" }

        return RankingResponseDto(
            myPosition = myEntry?.position ?: 0,
            myTotalXp = myEntry?.totalXp ?: 0L,
            top = top,
            lastUpdated = LocalDateTime.now().format(FORMATTER)
        )
    }

    @Transactional(readOnly = true)
    fun getMunicipalRanking(currentUserId: UUID, cityId: String): RankingResponseDto {
        val sql = """
            SELECT 
                u.user_id,
                u.username,
                u.nickname,
                COALESCE(SUM(x.xp_amount), 0) as total_xp
            FROM user_profile u
            LEFT JOIN xp_transactions x ON u.user_id = x.user_id
            WHERE u.city_id = :cityId
            GROUP BY u.user_id, u.username, u.nickname
            ORDER BY total_xp DESC
        """.trimIndent()

        val results = entityManager.createNativeQuery(sql)
            .setParameter("cityId", cityId)
            .resultList as List<Array<*>>

        val entries = results.mapIndexed { index, row ->
            val userId = (row[0] as java.util.UUID)
            val username = (row[1] as? String) ?: ""
            val nickname = (row[2] as? String)
            val totalXp = (row[3] as Number).toLong()

            val displayName = nickname?.takeIf { it.isNotBlank() } 
                ?: username.takeIf { it.isNotBlank() } 
                ?: "Usuario ${userId.toString().take(8)}"

            RankingEntryDto(
                position = index + 1,
                userId = userId.toString(),
                totalXp = totalXp,
                username = displayName
            )
        }

        val top = entries.take(RankingConstants.TOP_GLOBAL_SIZE)
        val myEntry = entries.find { it.userId == currentUserId.toString() }

        logger.info { "Municipal ranking calculated on-demand: city=$cityId, ${entries.size} users, myPos=${myEntry?.position ?: 0}" }

        return RankingResponseDto(
            myPosition = myEntry?.position ?: 0,
            myTotalXp = myEntry?.totalXp ?: 0L,
            top = top,
            lastUpdated = LocalDateTime.now().format(FORMATTER)
        )
    }
}