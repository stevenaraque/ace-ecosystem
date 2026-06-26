package sena.adso.ace_backend.ranking.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * FIXME-MVP-OBSOLETO: Entity de tabla materializada ranking_global.
 * El ranking ahora se calcula on-demand via SQL nativo en RankingQueryService.
 * La tabla queda en PostgreSQL pero no se lee ni escribe desde código.
 * Conservada para posible reactivación futura con job batch.
 */
@Deprecated(
    message = "Reemplazado por ranking on-demand. No usar en MVP.",
    replaceWith = ReplaceWith("Consulta directa a xp_transactions + user_profile")
)
@Entity
@Table(name = "ranking_global")
data class RankingGlobal(
    @Id
    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var totalXp: Long,

    @Column(nullable = false)
    var rankName: String,

    @Column(nullable = false)
    var position: Int,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)