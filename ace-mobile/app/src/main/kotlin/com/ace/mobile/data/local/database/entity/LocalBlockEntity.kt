package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bloque de ejercicio de ~300 segundos (5 minutos).
 * Apéndice S2, S3, S5.
 *
 * Estados: PENDING, SYNCING, SYNCED, ERROR
 */
@Entity(tableName = "local_blocks")
data class LocalBlockEntity(
    @PrimaryKey
    val blockId: String,              // UUIDv4 generado por el móvil (idempotencia)

    val sessionId: String,            // FK a local_sessions

    val timestampStart: Long,         // Epoch millis - fuente de verdad temporal
    val timestampEnd: Long,
    val durationSeconds: Int,         // ~300s (tolerancia ±10%: 270-330)

    val avgBpm: Double,               // Promedio de FC del bloque
    val maxBpm: Double,               // Máximo de FC
    val minBpm: Double,               // Mínimo de FC
    val sampleCount: Int,             // Cantidad de muestras (coherente con duración)

    val sportType: String,            // RUNNING, CYCLING, etc.

    val xpCalculated: Int,            // XP calculada localmente (S5)

    val status: String = "PENDING"    // PENDING, SYNCING, SYNCED, ERROR
)