package sena.adso.ace_backend.domain.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val tokenHash: String,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val deviceId: String,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column
    var revokedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by")
    var replacedBy: RefreshToken? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)