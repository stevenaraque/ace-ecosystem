package sena.adso.ace_backend.auth.service

import com.ace.shared.constants.AuthConstants
import sena.adso.ace_backend.auth.model.RefreshToken
import sena.adso.ace_backend.auth.repository.RefreshTokenRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
    }

    @Transactional
    fun createRefreshToken(userId: UUID, deviceId: String): String {
        val rawToken = generateSecureToken()
        val tokenHash = hashToken(rawToken)

        val refreshToken = RefreshToken(
            tokenHash = tokenHash,
            userId = userId,
            deviceId = deviceId,
            expiresAt = Instant.now().plus(AuthConstants.REFRESH_TOKEN_TTL_DAYS.toLong(), ChronoUnit.DAYS)
        )

        refreshTokenRepository.save(refreshToken)
        return rawToken
    }

    @Transactional
    fun rotateRefreshToken(rawToken: String, deviceId: String): Pair<String, UUID> {
        val tokenHash = hashToken(rawToken)
        val existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw IllegalArgumentException(AuthConstants.ERROR_REFRESH_INVALID)

        if (existingToken.revokedAt != null) {
            logger.warn { "Refresh token reused: hash=${tokenHash.take(8)}..., user=${existingToken.userId}" }
            throw IllegalArgumentException(AuthConstants.ERROR_REFRESH_REUSED)
        }

        if (existingToken.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException(AuthConstants.ERROR_REFRESH_INVALID)
        }

        if (existingToken.deviceId != deviceId) {
            throw IllegalArgumentException("Device mismatch")
        }

        // Revocar el actual
        existingToken.revokedAt = Instant.now()

        // Crear nuevo
        val newRawToken = generateSecureToken()
        val newTokenHash = hashToken(newRawToken)
        val newRefreshToken = RefreshToken(
            tokenHash = newTokenHash,
            userId = existingToken.userId,
            deviceId = deviceId,
            expiresAt = Instant.now().plus(AuthConstants.REFRESH_TOKEN_TTL_DAYS.toLong(), ChronoUnit.DAYS)
        )

        val savedNewToken = refreshTokenRepository.save(newRefreshToken)
        existingToken.replacedBy = savedNewToken
        refreshTokenRepository.save(existingToken)

        return newRawToken to existingToken.userId
    }

    @Transactional
    fun revokeToken(rawToken: String) {
        val tokenHash = hashToken(rawToken)
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        token.revokedAt = Instant.now()
        refreshTokenRepository.save(token)
    }

    @Transactional
    fun revokeAllUserTokens(userId: UUID) {
        val tokens = refreshTokenRepository.findAll()
            .filter { it.userId == userId && it.revokedAt == null }
        tokens.forEach { it.revokedAt = Instant.now() }
        refreshTokenRepository.saveAll(tokens)
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
