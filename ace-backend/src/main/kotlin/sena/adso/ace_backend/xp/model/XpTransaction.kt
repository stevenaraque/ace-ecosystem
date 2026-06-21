package sena.adso.ace_backend.xp.model

import com.ace.shared.enums.SportType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "xp_transactions")
data class XpTransaction(
    @Id
    @Column(nullable = false, updatable = false)
    val transactionId: UUID,

    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false, updatable = false)
    val blockId: UUID,

    @Column(nullable = false, updatable = false)
    val sessionId: UUID,

    @Column(nullable = false)
    val xpAmount: Int,

    @Column(nullable = false)
    val balanceAfter: Int,

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    @Column(nullable = false, updatable = false)
    val durationSeconds: Int,

    @Column(nullable = false, updatable = false)
    val avgBpm: Double,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant
)