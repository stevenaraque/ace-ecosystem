package sena.adso.ace_backend.user.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_stats")
data class UserStats(
    @Id
    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false)
    var totalXp: Long = 0,

    @Column(nullable = false)
    var totalSessions: Int = 0,

    @Column(nullable = false)
    var totalBlocks: Int = 0,

    @Column(nullable = false, name = "total_duration_seconds")
    var totalDurationSeconds: Long = 0,

    @Column(name = "avg_bpm_all_time")
    var avgBpmAllTime: Double? = null,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)