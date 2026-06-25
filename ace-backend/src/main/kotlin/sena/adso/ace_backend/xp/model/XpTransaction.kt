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
    val transactionId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val blockId: UUID,

    @Column(nullable = false)
    val sessionId: UUID,

    @Column(name = "xp_amount", nullable = false)
    val xpAmount: Int,

    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Long,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    @Column(nullable = false)
    val durationSeconds: Int,

    @Column(nullable = false)
    val avgBpm: Double,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)