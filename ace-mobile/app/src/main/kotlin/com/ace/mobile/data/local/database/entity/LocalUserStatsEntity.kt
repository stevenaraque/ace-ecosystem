package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estadísticas persistentes de perfil (procesadas, con XP).
 * Apéndice S10.
 *
 * Se actualizan inmediatamente al cerrar cada bloque (offline).
 * Se sincronizan con el backend en cada batch.
 */
@Entity(tableName = "local_user_stats")
data class LocalUserStatsEntity(
    @PrimaryKey
    val userId: String,               // UUID del usuario

    val totalXp: Int = 0,             // XP total acumulada

    val totalSessions: Int = 0,       // Sesiones completadas

    val totalBlocks: Int = 0,         // Bloques cerrados

    val totalDurationSeconds: Long = 0, // Tiempo total de ejercicio (segundos)

    val avgBpmAllTime: Double = 0.0,  // Promedio ponderado de FC histórico

    val lastSyncAt: Long? = null      // Última sincronización exitosa
)