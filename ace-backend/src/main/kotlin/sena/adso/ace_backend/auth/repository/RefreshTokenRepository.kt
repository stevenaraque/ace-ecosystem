package sena.adso.ace_backend.auth.repository

import sena.adso.ace_backend.auth.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Query("""
        SELECT rt FROM RefreshToken rt 
        WHERE rt.userId = :userId 
        AND rt.deviceId = :deviceId 
        AND rt.revokedAt IS NULL 
        AND rt.expiresAt > CURRENT_TIMESTAMP 
        ORDER BY rt.createdAt DESC 
    """)
    fun findValidTokenByUserAndDevice(userId: UUID, deviceId: String): RefreshToken?
}
