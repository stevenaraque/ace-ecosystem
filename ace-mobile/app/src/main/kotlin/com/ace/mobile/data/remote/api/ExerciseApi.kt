package com.ace.mobile.data.remote.api

import com.ace.shared.dto.SyncBatchRequestDto
import com.ace.shared.dto.SyncBatchResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ExerciseApi {

    /**
     * POST /api/exercise/batch
     */
    @POST("/api/exercise/batch")  // ← FIX: URL correcta con /api prefix
    suspend fun syncBatch(
        @Body request: SyncBatchRequestDto
    ): Response<SyncBatchResponseDto>
}