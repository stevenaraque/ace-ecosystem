package sena.adso.ace_backend.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.user.model.UserProfile
import java.util.UUID

@Repository
interface UserProfileRepository : JpaRepository<UserProfile, UUID> {

    fun findByUsername(username: String): UserProfile?

    fun findByCityId(cityId: String): List<UserProfile>
}