package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_user")
data class LocalUserEntity(
    @PrimaryKey
    val userId: String,
    val email: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenExpiresAt: Long? = null,
    val deviceId: String,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastExerciseDate: Long? = null,
    val totalXp: Long = 0L,
    val totalSessions: Int = 0  // ← NUEVO
)