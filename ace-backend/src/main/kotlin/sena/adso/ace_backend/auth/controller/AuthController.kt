package sena.adso.ace_backend.auth.controller

import com.ace.shared.constants.AuthConstants
import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import com.ace.shared.dto.RefreshTokenRequestDto
import sena.adso.ace_backend.auth.service.AuthService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequestDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Login failed for ${request.email}: ${e.message}" }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequestDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = authService.register(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Registration failed for ${request.email}: ${e.message}" }
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequestDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = authService.refresh(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            val errorCode = e.message
            val status = when (errorCode) {
                AuthConstants.ERROR_REFRESH_REUSED -> HttpStatus.UNAUTHORIZED
                AuthConstants.ERROR_REFRESH_REVOKED -> HttpStatus.UNAUTHORIZED
                AuthConstants.ERROR_REFRESH_INVALID -> HttpStatus.UNAUTHORIZED
                else -> HttpStatus.BAD_REQUEST
            }
            logger.warn { "Refresh failed: $errorCode, device: ${request.deviceId}" }
            ResponseEntity.status(status).build()
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("X-Refresh-Token") refreshToken: String): ResponseEntity<Void> {
        authService.logout(refreshToken)
        return ResponseEntity.ok().build()
    }
}
