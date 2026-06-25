package sena.adso.ace_backend.user.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "user_profiles")
data class UserProfile(
    @Id
    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column
    var username: String? = null,

    @Column
    var nickname: String? = null,

    @Column
    var cityId: String? = null,

    @Column(name = "weight_kg")
    var weightKg: java.math.BigDecimal? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)