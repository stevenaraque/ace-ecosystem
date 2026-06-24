package sena.adso.ace_backend.user.service

import com.ace.shared.dto.UpdateProfileRequestDto
import com.ace.shared.dto.UserProfileDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.user.model.UserProfile
import sena.adso.ace_backend.user.repository.UserProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ProfileService(
    private val userProfileRepository: UserProfileRepository
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserProfileDto {
        val profile = userProfileRepository.findById(userId).orElseGet {
            logger.info { "Lazy init profile for user $userId" }
            val newProfile = UserProfile(userId = userId)
            userProfileRepository.save(newProfile)
        }

        return UserProfileDto(
            userId = profile.userId.toString(),
            username = profile.username ?: "",
            nickname = profile.nickname,
            cityId = profile.cityId,
            weightKg = profile.weightKg?.toDouble(),
            birthDate = profile.birthDate?.toString()
        )
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProfileRequestDto): UserProfileDto {
        val profile = userProfileRepository.findById(userId).orElseGet {
            logger.info { "Lazy init profile for user $userId on update" }
            UserProfile(userId = userId)
        }

        request.username?.let { profile.username = it }
        request.nickname?.let { profile.nickname = it }
        request.cityId?.let { profile.cityId = it }
        request.weightKg?.let { profile.weightKg = java.math.BigDecimal.valueOf(it) }
        request.birthDate?.let { profile.birthDate = LocalDate.parse(it) }

        profile.updatedAt = Instant.now()
        userProfileRepository.save(profile)

        logger.info { "Profile updated for user $userId" }

        return UserProfileDto(
            userId = profile.userId.toString(),
            username = profile.username ?: "",
            nickname = profile.nickname,
            cityId = profile.cityId,
            weightKg = profile.weightKg?.toDouble(),
            birthDate = profile.birthDate?.toString()
        )
    }
}