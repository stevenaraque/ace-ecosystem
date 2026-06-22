package com.ace.mobile.domain.usecase.xp

import android.util.Log
import com.ace.mobile.data.local.database.dao.XpFormulaDao
import com.ace.mobile.data.local.database.entity.LocalXpFormulaEntity
import com.ace.mobile.data.remote.api.XpFormulaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CacheXpFormulasUseCase @Inject constructor(
    private val xpFormulaApi: XpFormulaApi,
    private val xpFormulaDao: XpFormulaDao
) {

    suspend operator fun invoke(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d("CacheXpFormulas", "Fetching formulas from backend...")
            val response = xpFormulaApi.getFormulas()

            if (!response.isSuccessful) {
                Log.w("CacheXpFormulas", "Failed: HTTP ${response.code()}")
                return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            }

            val formulas = response.body() ?: emptyList()
            if (formulas.isEmpty()) {
                Log.w("CacheXpFormulas", "Empty response")
                return@withContext Result.failure(Exception("No formulas received"))
            }

            // Guardar en Room
            xpFormulaDao.clearAll()
            xpFormulaDao.insertAll(
                formulas.map { dto ->
                    LocalXpFormulaEntity(
                        sportType = dto.sportType.name,
                        minBpm = dto.minBpm.toDouble(),
                        xpPerMinute = dto.xpPerMinute.toDouble(),
                        maxXpPerBlock = dto.maxXpPerBlock
                    )
                }
            )

            Log.d("CacheXpFormulas", "Cached ${formulas.size} formulas")
            Result.success(formulas.size)

        } catch (e: Exception) {
            Log.e("CacheXpFormulas", "Error caching formulas", e)
            Result.failure(e)
        }
    }
}