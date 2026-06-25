package sena.adso.ace_backend.auth.service

import com.ace.shared.constants.AuthConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sena.adso.ace_backend.auth.model.RefreshToken
import sena.adso.ace_backend.auth.repository.RefreshTokenRepository
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshTokenServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var refreshTokenService: RefreshTokenService

    @BeforeEach
    fun setup() {
        refreshTokenRepository = mockk(relaxed = true)
        refreshTokenService = RefreshTokenService(refreshTokenRepository)
    }

    @Test
    fun `create refresh token - returns raw token and saves hash`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"

        every { refreshTokenRepository.save(any()) } answers { arg(0) }

        val rawToken = refreshTokenService.createRefreshToken(userId, deviceId)

        assertNotNull(rawToken)
        assertTrue(rawToken.isNotBlank())

        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `rotate - success`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"
        val oldHash = hashToken("old-raw-token")

        val oldToken = RefreshToken(
            tokenHash = oldHash,
            userId = userId,
            deviceId = deviceId,
            expiresAt = Instant.now().plusSeconds(3600)
        )

        every { refreshTokenRepository.findByTokenHash(oldHash) } returns oldToken
        every { refreshTokenRepository.save(any()) } answers { arg(0) }

        val (newRawToken, returnedUserId) = refreshTokenService.rotateRefreshToken("old-raw-token", deviceId)

        assertNotNull(newRawToken)
        assertEquals(userId, returnedUserId)
        assertTrue(oldToken.revokedAt != null) // Fue revocado

        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `rotate - token already revoked throws REFRESH_REUSED`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"
        val hash = hashToken("revoked-token")

        val revokedToken = RefreshToken(
            tokenHash = hash,
            userId = userId,
            deviceId = deviceId,
            expiresAt = Instant.now().plusSeconds(3600),
            revokedAt = Instant.now()
        )

        every { refreshTokenRepository.findByTokenHash(hash) } returns revokedToken

        val exception = assertThrows<IllegalArgumentException> {
            refreshTokenService.rotateRefreshToken("revoked-token", deviceId)
        }

        assertEquals(AuthConstants.ERROR_REFRESH_REUSED, exception.message)
    }

    @Test
    fun `rotate - expired token throws REFRESH_INVALID`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"
        val hash = hashToken("expired-token")

        val expiredToken = RefreshToken(
            tokenHash = hash,
            userId = userId,
            deviceId = deviceId,
            expiresAt = Instant.now().minusSeconds(3600) // Expirado
        )

        every { refreshTokenRepository.findByTokenHash(hash) } returns expiredToken

        val exception = assertThrows<IllegalArgumentException> {
            refreshTokenService.rotateRefreshToken("expired-token", deviceId)
        }

        assertEquals(AuthConstants.ERROR_REFRESH_INVALID, exception.message)
    }

    @Test
    fun `rotate - token not found throws REFRESH_INVALID`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        val exception = assertThrows<IllegalArgumentException> {
            refreshTokenService.rotateRefreshToken("nonexistent", "device")
        }

        assertEquals(AuthConstants.ERROR_REFRESH_INVALID, exception.message)
    }

    @Test
    fun `revoke token - marks as revoked`() {
        val hash = hashToken("token-to-revoke")
        val token = RefreshToken(
            tokenHash = hash,
            userId = UUID.randomUUID(),
            deviceId = "device",
            expiresAt = Instant.now().plusSeconds(3600)
        )

        every { refreshTokenRepository.findByTokenHash(hash) } returns token
        every { refreshTokenRepository.save(any()) } answers { arg(0) }

        refreshTokenService.revokeToken("token-to-revoke")

        assertNotNull(token.revokedAt)
        verify { refreshTokenRepository.save(any()) }
    }

    // ← NUEVO TEST F7
    @Test
    fun `revokeAllUserTokens - revokes all active tokens for user`() {
        val userId = UUID.randomUUID()
        val token1 = RefreshToken(
            tokenHash = "hash1",
            userId = userId,
            deviceId = "device-1",
            expiresAt = Instant.now().plusSeconds(3600)
        )
        val token2 = RefreshToken(
            tokenHash = "hash2",
            userId = userId,
            deviceId = "device-2",
            expiresAt = Instant.now().plusSeconds(3600)
        )
        val otherUserToken = RefreshToken(
            tokenHash = "hash3",
            userId = UUID.randomUUID(), // otro usuario
            deviceId = "device-3",
            expiresAt = Instant.now().plusSeconds(3600)
        )

        every { refreshTokenRepository.findAll() } returns listOf(token1, token2, otherUserToken)
        every { refreshTokenRepository.saveAll(any<List<RefreshToken>>()) } answers { arg(0) }

        refreshTokenService.revokeAllUserTokens(userId)

        assertNotNull(token1.revokedAt)
        assertNotNull(token2.revokedAt)
        assertTrue(otherUserToken.revokedAt == null) // No se toca el de otro usuario

        verify { refreshTokenRepository.saveAll(any<List<RefreshToken>>()) }
    }

    private fun hashToken(token: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}