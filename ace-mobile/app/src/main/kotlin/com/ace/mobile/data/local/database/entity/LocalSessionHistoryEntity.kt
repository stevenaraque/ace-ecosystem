package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historial de las últimas 5 sesiones completadas (FIFO).
 * Apéndice S9.
 *
 * Datos raw (sin procesar): duración, FC promedio, tipo, XP.
 * No discrimina categoría (Running, Cycling, etc. se muestran juntas).
 */
@Entity(tableName = "local_session_history")
data class LocalSessionHistoryEntity(
    @PrimaryKey
    val sessionId: String,            // UUID de la sesión

    val timestampStart: Long,         // Inicio
    val timestampEnd: Long,           // Fin

    val sportType: String,            // Tipo de actividad

    val durationSeconds: Int,         // Duración total

    val avgBpm: Double,               // FC promedio de la sesión

    val totalBlocks: Int,             // Cantidad de bloques

    val totalXp: Int                  // XP total ganada
)