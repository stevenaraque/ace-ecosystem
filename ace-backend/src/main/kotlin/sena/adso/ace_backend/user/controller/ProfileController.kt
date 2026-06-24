package sena.adso.ace_backend.user.controller

import com.ace.shared.dto.UpdateProfileRequestDto
import com.ace.shared.dto.UserProfileDto
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import sena.adso.ace_backend.user.service.ProfileService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val profileService: ProfileService
) {

    @GetMapping
    fun getProfile(
        @AuthenticationPrincipal userDetails: String
    ): ResponseEntity<UserProfileDto> {
        val userId = UUID.fromString(userDetails)
        logger.info { "Profile requested for user $userId" }
        return ResponseEntity.ok(profileService.getProfile(userId))
    }

    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal userDetails: String,
        @RequestBody request: UpdateProfileRequestDto
    ): ResponseEntity<UserProfileDto> {
        val userId = UUID.fromString(userDetails)
        logger.info { "Profile update requested for user $userId" }
        return ResponseEntity.ok(profileService.updateProfile(userId, request))
    }
}