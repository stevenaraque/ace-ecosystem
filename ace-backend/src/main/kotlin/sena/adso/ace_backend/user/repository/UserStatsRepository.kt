package sena.adso.ace_backend.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.user.model.UserStats
import java.util.UUID

@Repository
interface UserStatsRepository : JpaRepository<UserStats, UUID> {

    fun findByUserId(userId: UUID): UserStats?
}