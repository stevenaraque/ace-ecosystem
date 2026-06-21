package sena.adso.ace_backend.auth.service

import com.ace.shared.constants.AuthConstants
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.logging.Logger
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.issuer}") private val issuer: String
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    private val logger = Logger.getLogger(JwtService::class.java.name)

    fun generateAccessToken(userId: UUID, deviceId: String): String {
        val now = Instant.now()
        val expiration = now.plus(AuthConstants.ACCESS_TOKEN_TTL_MINUTES.toLong(), ChronoUnit.MINUTES)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("deviceId", deviceId)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact()
    }

    fun validateAccessToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            logger.warning("JWT validation failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    fun extractUserId(token: String): UUID? {
        return validateAccessToken(token)?.subject?.let { UUID.fromString(it) }
    }

    fun extractDeviceId(token: String): String? {
        return validateAccessToken(token)?.get("deviceId") as? String
    }

    fun isTokenExpired(token: String): Boolean {
        val claims = validateAccessToken(token) ?: return true
        return claims.expiration.before(Date.from(Instant.now()))
    }
}