package com.ace.mobile.feature.stats.data

import com.ace.shared.dto.StatsReconcileRequestDto
import com.ace.shared.dto.StatsReconcileResponseDto
import com.ace.shared.dto.StatsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StatsApi {

    @GET("api/stats")
    suspend fun getStats(): Response<StatsResponseDto>

    @POST("api/stats/reconcile")
    suspend fun reconcile(
        @Body request: StatsReconcileRequestDto
    ): Response<StatsReconcileResponseDto>
}