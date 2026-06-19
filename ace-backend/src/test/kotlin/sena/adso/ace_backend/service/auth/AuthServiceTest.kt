package sena.adso.ace_backend.auth.service

import com.ace.shared.constants.AuthConstants
import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import com.ace.shared.dto.RefreshTokenRequestDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mindrot.jbcrypt.BCrypt
import sena.adso.ace_backend.auth.model.User
import sena.adso.ace_backend.auth.repository.UserRepository
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtService: JwtService
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        jwtService = mockk()
        refreshTokenService = mockk()
        authService = AuthService(userRepository, jwtService, refreshTokenService)
    }

    @Test
    fun `login - success`() {
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            email = "test@ace.com",
            passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt())
        )
        val deviceId = "device-001"

        every { userRepository.findByEmail("test@ace.com") } returns user
        every { jwtService.generateAccessToken(userId, deviceId) } returns "access-token-123"
        every { refreshTokenService.createRefreshToken(userId, deviceId) } returns "refresh-token-456"

        val request = AuthRequestDto(
            email = "test@ace.com",
            password = "password123",
            deviceId = deviceId
        )
        val result = authService.login(request)

        assertNotNull(result)
        assertEquals("access-token-123", result.accessToken)
        assertEquals("refresh-token-456", result.refreshToken)
        assertEquals(userId.toString(), result.userId)
        assertEquals("test@ace.com", result.email)
        assertEquals(900L, result.expiresIn) // 15 min * 60 = 900

        verify { userRepository.findByEmail("test@ace.com") }
        verify { jwtService.generateAccessToken(userId, deviceId) }
        verify { refreshTokenService.createRefreshToken(userId, deviceId) }
    }

    @Test
    fun `login - wrong password`() {
        val user = User(
            email = "test@ace.com",
            passwordHash = BCrypt.hashpw("correct-password", BCrypt.gensalt())
        )

        every { userRepository.findByEmail("test@ace.com") } returns user

        val request = AuthRequestDto(
            email = "test@ace.com",
            password = "wrong-password",
            deviceId = "device-001"
        )

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `login - user not found`() {
        every { userRepository.findByEmail("ghost@ace.com") } returns null

        val request = AuthRequestDto(
            email = "ghost@ace.com",
            password = "password123",
            deviceId = "device-001"
        )

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `login - deviceId null should throw`() {
        val user = User(
            email = "test@ace.com",
            passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt())
        )
        every { userRepository.findByEmail("test@ace.com") } returns user

        val request = AuthRequestDto(
            email = "test@ace.com",
            password = "password123",
            deviceId = null // nullable en :shared
        )

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }

    @Test
    fun `register - success`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"

        every { userRepository.existsByEmail("new@ace.com") } returns false
        every { userRepository.save(any()) } answers {
            val user = arg<User>(0)
            user.copy(id = userId)
        }
        every { jwtService.generateAccessToken(userId, deviceId) } returns "access-token-789"
        every { refreshTokenService.createRefreshToken(userId, deviceId) } returns "refresh-token-abc"

        val request = AuthRequestDto(
            email = "new@ace.com",
            password = "password123",
            deviceId = deviceId
        )
        val result = authService.register(request)

        assertNotNull(result)
        assertEquals("access-token-789", result.accessToken)
        assertEquals("new@ace.com", result.email)

        verify { userRepository.existsByEmail("new@ace.com") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `register - email already exists`() {
        every { userRepository.existsByEmail("exists@ace.com") } returns true

        val request = AuthRequestDto(
            email = "exists@ace.com",
            password = "password123",
            deviceId = "device-001"
        )

        assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
    }

    @Test
    fun `refresh - success`() {
        val userId = UUID.randomUUID()
        val deviceId = "device-001"
        val newRefreshToken = "new-refresh-123"
        val accessToken = "new-access-456"

        every { refreshTokenService.rotateRefreshToken("old-refresh", deviceId) } returns (newRefreshToken to userId)
        every { jwtService.generateAccessToken(userId, deviceId) } returns accessToken
        every { userRepository.findById(userId) } returns Optional.of(
            User(id = userId, email = "test@ace.com", passwordHash = "hash")
        )

        val request = RefreshTokenRequestDto(
            refreshToken = "old-refresh",
            deviceId = deviceId
        )
        val result = authService.refresh(request)

        assertEquals(accessToken, result.accessToken)
        assertEquals(newRefreshToken, result.refreshToken)
        assertEquals(userId.toString(), result.userId)

        verify { refreshTokenService.rotateRefreshToken("old-refresh", deviceId) }
    }

    @Test
    fun `logout - calls revoke`() {
        every { refreshTokenService.revokeToken("refresh-123") } returns Unit

        authService.logout("refresh-123")

        verify { refreshTokenService.revokeToken("refresh-123") }
    }
}