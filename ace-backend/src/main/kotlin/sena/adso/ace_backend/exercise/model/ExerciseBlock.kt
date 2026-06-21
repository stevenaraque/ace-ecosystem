package sena.adso.ace_backend.exercise.model

import com.ace.shared.enums.SportType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "exercise_blocks")
data class ExerciseBlock(
    @Id
    @Column(nullable = false, unique = true, updatable = false)
    val blockId: UUID,

    @Column(nullable = false, updatable = false)
    val sessionId: UUID,

    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false, updatable = false)
    val deviceId: String,

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    @Column(nullable = false, updatable = false)
    val timestampStart: Instant,

    @Column(nullable = false, updatable = false)
    val timestampEnd: Instant,

    @Column(nullable = false, updatable = false)
    val durationSeconds: Int,

    @Column(nullable = false, updatable = false)
    val avgBpm: Double,

    @Column(nullable = false, updatable = false)
    val maxBpm: Double,

    @Column(nullable = false, updatable = false)
    val minBpm: Double,

    @Column(nullable = false, updatable = false)
    val sampleCount: Int,

    @Column(nullable = false, updatable = false)
    val xpCalculated: Int,

    @Column(nullable = false, updatable = false)
    val schemaVersion: Int = 1,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)