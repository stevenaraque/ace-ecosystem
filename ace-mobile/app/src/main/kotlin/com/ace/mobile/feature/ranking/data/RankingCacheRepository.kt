package com.ace.mobile.feature.ranking.data

import android.util.Log
import com.ace.mobile.core.database.dao.RankingCacheDao
import com.ace.mobile.core.database.entity.LocalRankingCacheEntity
import com.ace.mobile.feature.ranking.data.RankingApi
import com.ace.shared.constants.RankingConstants
import com.ace.shared.dto.RankingEntryDto
import com.ace.shared.dto.RankingResponseDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RankingCacheRepo"

@Singleton
class RankingCacheRepository @Inject constructor(
    private val rankingApi: RankingApi,
    private val rankingCacheDao: RankingCacheDao,
    private val gson: Gson
) {

    // ← NUEVO: Flujo de invalidación para que RankingViewModel reaccione
    private val _invalidateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val invalidateSignal: SharedFlow<Unit> = _invalidateSignal.asSharedFlow()

    suspend fun getGlobalRanking(forceRefresh: Boolean = false): Result<RankingResponseDto> {
        return getRanking("GLOBAL", forceRefresh) {
            rankingApi.getGlobalRanking()
        }
    }

    suspend fun getMunicipalRanking(cityId: String, forceRefresh: Boolean = false): Result<RankingResponseDto> {
        return getRanking("MUNICIPAL_$cityId", forceRefresh) {
            rankingApi.getMunicipalRanking(cityId)
        }
    }

    /**
     * Invalida todo el cache de ranking. Llamar cuando:
     * - SyncBatchResponseDto.rankChanged = true
     * - Usuario cambia de ciudad (F2)
     * - Pull-to-refresh explícito
     */
    suspend fun invalidateCache() {
        Log.i(TAG, "Invalidating all ranking caches")
        rankingCacheDao.clearAll()
        _invalidateSignal.tryEmit(Unit)
    }

    private suspend fun getRanking(
        cacheType: String,
        forceRefresh: Boolean,
        apiCall: suspend () -> retrofit2.Response<RankingResponseDto>
    ): Result<RankingResponseDto> = withContext(Dispatchers.IO) {

        val now = System.currentTimeMillis()

        // 1. Intentar leer cache si no se fuerza refresh
        if (!forceRefresh) {
            val cached = rankingCacheDao.getByType(cacheType)
            if (cached != null && cached.validUntil > now) {
                Log.d(TAG, "Cache hit for $cacheType, valid until ${cached.validUntil}")
                return@withContext Result.success(cached.toResponseDto())
            }
        }

        // 2. Llamar API
        Log.d(TAG, "Fetching $cacheType from API")
        try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // 3. Guardar en cache (solo top 10 + mi posición)
                    val topToCache = body.top.take(RankingConstants.TOP_LOCAL_CACHE_SIZE)
                    val cacheEntity = LocalRankingCacheEntity(
                        type = cacheType,
                        myPosition = body.myPosition,
                        myTotalXp = body.myTotalXp.toInt(),
                        topJson = gson.toJson(topToCache),
                        cachedAt = now,
                        validUntil = now + (RankingConstants.RANKING_CACHE_TTL_HOURS * 60 * 60 * 1000)
                    )
                    rankingCacheDao.insertOrReplace(cacheEntity)

                    Log.i(TAG, "Cached $cacheType: myPos=${body.myPosition}, top=${topToCache.size}")
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e(TAG, "API error ${response.code()}: ${response.errorBody()?.string()}")
                // Fallback a cache stale si existe
                val stale = rankingCacheDao.getByType(cacheType)
                if (stale != null) {
                    Log.w(TAG, "Using stale cache for $cacheType")
                    Result.success(stale.toResponseDto())
                } else {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching $cacheType", e)
            val stale = rankingCacheDao.getByType(cacheType)
            if (stale != null) {
                Log.w(TAG, "Using stale cache for $cacheType after error")
                Result.success(stale.toResponseDto())
            } else {
                Result.failure(e)
            }
        }
    }

    private fun LocalRankingCacheEntity.toResponseDto(): RankingResponseDto {
        val topList: List<RankingEntryDto> = gson.fromJson(
            topJson,
            object : TypeToken<List<RankingEntryDto>>() {}.type
        ) ?: emptyList()

        return RankingResponseDto(
            myPosition = myPosition,
            myTotalXp = myTotalXp.toLong(),
            top = topList,
            lastUpdated = java.time.Instant.ofEpochMilli(cachedAt).toString()
        )
    }
}