package com.ace.mobile.feature.history.data

import com.ace.shared.dto.SessionHistoryEntryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface HistoryApi {

    @GET("api/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 20
    ): Response<List<SessionHistoryEntryDto>>
}