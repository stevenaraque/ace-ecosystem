package sena.adso.ace_backend.streak.model

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "user_streaks")
data class UserStreak(
    @Id
    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false)
    var currentStreak: Int = 0,

    @Column(nullable = false)
    var bestStreak: Int = 0,

    @Column
    var lastExerciseDate: LocalDate? = null
)