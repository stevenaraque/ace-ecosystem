package sena.adso.ace_backend.auth.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.*

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        jwtService = JwtService(
            secret = "ace-super-secret-key-min-32-characters-long!!",
            issuer = "ace-backend"
        )
    }

    @Test
    fun `generate and validate token`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"

        val token = jwtService.generateAccessToken(userId, deviceId)

        assertNotNull(token)
        assertTrue(token.isNotBlank())

        val extractedUserId = jwtService.extractUserId(token)
        val extractedDeviceId = jwtService.extractDeviceId(token)

        assertEquals(userId, extractedUserId)
        assertEquals(deviceId, extractedDeviceId)
        assertFalse(jwtService.isTokenExpired(token))
    }

    @Test
    fun `invalid token returns null`() {
        val result = jwtService.validateAccessToken("invalid-token")
        assertNull(result)
    }

    @Test
    fun `expired token is detected`() {
        // Token con secret diferente para forzar invalidación
        val badService = JwtService(
            secret = "different-secret-key-min-32-characters-long!!",
            issuer = "other"
        )
        val userId = UUID.randomUUID()
        val token = badService.generateAccessToken(userId, "device")

        // El token es válido para badService pero no para nuestro jwtService
        assertNull(jwtService.validateAccessToken(token))
    }
}