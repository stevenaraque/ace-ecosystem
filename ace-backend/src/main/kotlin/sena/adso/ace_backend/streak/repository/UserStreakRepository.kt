package sena.adso.ace_backend.streak.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.streak.model.UserStreak
import java.util.UUID

@Repository
interface UserStreakRepository : JpaRepository<UserStreak, UUID>