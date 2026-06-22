package sena.adso.ace_backend.xp.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rank_catalog")
data class RankCatalog(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val rankName: String,

    @Column(nullable = false)
    val minXp: Int,

    @Column
    val maxXp: Int? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)