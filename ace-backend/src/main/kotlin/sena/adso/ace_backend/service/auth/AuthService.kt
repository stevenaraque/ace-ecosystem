package sena.adso.ace_backend.service.auth

import com.ace.shared.constants.AuthConstants
import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import com.ace.shared.dto.RefreshTokenRequestDto
import sena.adso.ace_backend.domain.user.User
import sena.adso.ace_backend.repository.UserRepository
import mu.KotlinLogging
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService
) {

    @Transactional
    fun login(request: AuthRequestDto): AuthResponseDto {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        // deviceId puede ser null en AuthRequestDto — usamos fallback
        val deviceId = request.deviceId ?: throw IllegalArgumentException("Device ID required")

        val accessToken = jwtService.generateAccessToken(user.id!!, deviceId)
        val refreshToken = refreshTokenService.createRefreshToken(user.id, deviceId)

        logger.info { "User logged in: ${user.email}, device: $deviceId" }

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = (AuthConstants.ACCESS_TOKEN_TTL_MINUTES * 60).toLong(), // Long, no Int
            userId = user.id.toString(),
            email = user.email
        )
    }

    @Transactional
    fun register(request: AuthRequestDto): AuthResponseDto {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val deviceId = request.deviceId ?: throw IllegalArgumentException("Device ID required")

        val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(12))
        val user = User(
            email = request.email,
            passwordHash = passwordHash
        )
        val savedUser = userRepository.save(user)

        val accessToken = jwtService.generateAccessToken(savedUser.id!!, deviceId)
        val refreshToken = refreshTokenService.createRefreshToken(savedUser.id, deviceId)

        logger.info { "User registered: ${savedUser.email}" }

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = (AuthConstants.ACCESS_TOKEN_TTL_MINUTES * 60).toLong(),
            userId = savedUser.id.toString(),
            email = savedUser.email
        )
    }

    @Transactional
    fun refresh(request: RefreshTokenRequestDto): AuthResponseDto {
        val (newRefreshToken, userId) = refreshTokenService.rotateRefreshToken(
            request.refreshToken,
            request.deviceId
        )

        val accessToken = jwtService.generateAccessToken(userId, request.deviceId)
        val user = userRepository.findById(userId).orElseThrow()

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            expiresIn = (AuthConstants.ACCESS_TOKEN_TTL_MINUTES * 60).toLong(),
            userId = userId.toString(),
            email = user.email
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenService.revokeToken(refreshToken)
        logger.info { "User logged out" }
    }
}