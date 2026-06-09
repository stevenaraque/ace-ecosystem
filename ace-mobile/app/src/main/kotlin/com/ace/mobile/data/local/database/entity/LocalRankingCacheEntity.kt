package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache del ranking (global y municipal).
 * Apéndice S6.
 *
 * Válido por 1 hora. Se invalida si rank_changed = true en sync.
 */
@Entity(tableName = "local_ranking_cache")
data class LocalRankingCacheEntity(
    @PrimaryKey
    val type: String,                 // "GLOBAL" o "MUNICIPAL_{cityId}"

    val myPosition: Int,              // Posición del usuario

    val myTotalXp: Int,               // XP total en ese momento

    val topJson: String,              // Top 10 serializado en JSON

    val cachedAt: Long,               // Epoch millis de cuando se cacheó

    val validUntil: Long              // cachedAt + 1 hora (en millis)
)