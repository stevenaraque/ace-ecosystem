package sena.adso.ace_backend.xp.model

import com.ace.shared.enums.SportType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "xp_formulas")
data class XpFormula(
    @Id
    @Column(nullable = false, updatable = false)
    val formulaId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    @Column(nullable = false)
    val minBpm: Double,

    @Column(nullable = false)
    val xpPerMinute: Double,

    @Column(nullable = false)
    val maxXpPerBlock: Int,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val version: Int = 1,  // ← AÑADIDO para M4

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)