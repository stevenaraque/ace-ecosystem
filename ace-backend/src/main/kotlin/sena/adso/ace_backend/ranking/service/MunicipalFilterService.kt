package sena.adso.ace_backend.ranking.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import sena.adso.ace_backend.user.model.UserProfile
import sena.adso.ace_backend.user.repository.UserProfileRepository
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class MunicipalFilterService(
    private val userProfileRepository: UserProfileRepository
) {

    fun getProfilesWithCity(): List<UserProfile> {
        val profiles = userProfileRepository.findAll()
            .filter { !it.cityId.isNullOrBlank() }
        
        logger.info { "Municipal filter: ${profiles.size} users with cityId" }
        return profiles
    }

    fun getCityIdForUser(userId: UUID): String? {
        return userProfileRepository.findById(userId)
            .orElse(null)?.cityId
    }

    fun getDisplayName(profile: UserProfile): String {
        return profile.nickname?.takeIf { it.isNotBlank() }
            ?: profile.username?.takeIf { it.isNotBlank() }
            ?: "Usuario ${profile.userId.toString().take(8)}"
    }
}