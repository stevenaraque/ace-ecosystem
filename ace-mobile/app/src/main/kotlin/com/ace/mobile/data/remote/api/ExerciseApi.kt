package com.ace.mobile.data.remote.api

import com.ace.shared.dto.SyncBatchRequestDto
import com.ace.shared.dto.SyncBatchResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API de ejercicio / sincronización.
 * Endpoint protegido con JWT (agregado por AuthInterceptor).
 */
interface ExerciseApi {

    /**
     * POST /api/exercise/batch
     * Envía un batch de bloques pendientes al backend.
     *
     * @param request SyncBatchRequestDto con hasta 20 bloques + clientStats
     * @return 201 Created con SyncBatchResponseDto (acceptedBlocks, rejectedBlocks, stats, streak)
     *         422 Unprocessable si algún bloque falla validación
     *         401 si el token expiró (AuthInterceptor maneja refresh)
     */
    @POST("exercise/batch")
    suspend fun syncBatch(
        @Body request: SyncBatchRequestDto
    ): Response<SyncBatchResponseDto>
}