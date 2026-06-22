package com.ace.mobile.data.remote.api

import com.ace.shared.dto.RankingResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RankingApi {

    @GET("api/ranking/global")
    suspend fun getGlobalRanking(): Response<RankingResponseDto>

    @GET("api/ranking/municipal")
    suspend fun getMunicipalRanking(
        @Query("cityId") cityId: String
    ): Response<RankingResponseDto>
}