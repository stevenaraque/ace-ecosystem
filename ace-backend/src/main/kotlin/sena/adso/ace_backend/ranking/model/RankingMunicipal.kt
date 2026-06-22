package sena.adso.ace_backend.ranking.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "ranking_municipal")
data class RankingMunicipal(
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
    var cityId: String,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)