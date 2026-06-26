package sena.adso.ace_backend.ranking.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.ranking.model.RankingGlobal
import java.util.UUID

/**
 * FIXME-MVP-OBSOLETO: Repositorio de tabla materializada ranking_global.
 * El ranking ahora se calcula on-demand en RankingQueryService.
 * Conservado para posible reactivación futura con tablas materializadas.
 */
@Deprecated(
    message = "Reemplazado por ranking on-demand. No usar en MVP.",
    replaceWith = ReplaceWith("RankingQueryService")
)
@Repository
interface RankingGlobalRepository : JpaRepository<RankingGlobal, UUID> {

    fun findAllByOrderByPositionAsc(): List<RankingGlobal>

    fun findByUserId(userId: UUID): RankingGlobal?

    @Modifying
    @Query("DELETE FROM RankingGlobal")
    fun deleteAllEntries()
}