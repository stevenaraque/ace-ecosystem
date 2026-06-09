package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Usuario logueado + tokens + cache de racha.
 * Apéndice S4 (Auth), S7 (Streaks).
 *
 * NOTA: Solo 1 registro en esta tabla (el usuario actual).
 */
@Entity(tableName = "local_user")
data class LocalUserEntity(
    @PrimaryKey
    val userId: String,               // UUID del usuario

    val email: String? = null,        // Email (opcional, no se guarda password)

    // ─── Tokens JWT (S4) ───
    val accessToken: String? = null,  // JWT de acceso (15 min)
    val refreshToken: String? = null, // JWT de refresh (7 días)
    val tokenExpiresAt: Long? = null, // Epoch millis de expiración del access

    val deviceId: String,             // UUID generado por instalación

    // ─── Cache de racha (S7) ───
    val currentStreak: Int = 0,       // Racha actual
    val bestStreak: Int = 0,          // Mejor racha histórica
    val lastExerciseDate: Long? = null, // Epoch millis del último ejercicio

    // ─── XP total local (S10) ───
    val totalXp: Int = 0              // XP total acumulada (cache)
)