package sena.adso.ace_backend.exercise.model

import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "exercise_sessions")
data class ExerciseSession(
    @Id
    @Column(nullable = false, unique = true, updatable = false)
    val sessionId: UUID,

    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false, updatable = false)
    val deviceId: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val sportType: SportType,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SessionStatus,

    @Column(nullable = false, updatable = false)
    val timestampStart: Instant,

    @Column(nullable = true)
    var timestampEnd: Instant? = null,

    @Column(nullable = false)
    var totalBlocks: Int = 0,

    @Column(nullable = false)
    var totalXp: Int = 0,

    @Column(nullable = false, updatable = false)
    val schemaVersion: Int = 1,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)