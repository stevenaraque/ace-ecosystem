// xp/dto/RankInfoResponse.kt
package sena.adso.ace_backend.xp.dto

data class RankInfoResponse(
    val currentRank: String,
    val nextRank: String?,
    val xpToNextRank: Int?,
    val totalXp: Long
)