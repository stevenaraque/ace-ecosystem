package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesión de ejercicio completa.
 * Apéndice S2.
 *
 * Estados: ACTIVE, PAUSED, COMPLETED, ABORTED
 */
@Entity(tableName = "local_sessions")
data class LocalSessionEntity(
    @PrimaryKey
    val sessionId: String,            // UUID generado por el móvil

    val status: String,               // ACTIVE, PAUSED, COMPLETED, ABORTED

    val sportType: String,            // Tipo de actividad

    val timestampStart: Long,         // Inicio de la sesión
    val timestampEnd: Long? = null,   // Fin (null si está activa)

    val totalBlocks: Int = 0,         // Bloques generados en esta sesión
    val totalXp: Int = 0            // XP total acumulada en la sesión
)