package com.ace.mobile.data.remote.api

import com.ace.shared.dto.XpFormulaDto
import retrofit2.Response
import retrofit2.http.GET

interface XpFormulaApi {

    @GET("/api/xp/formulas")
    suspend fun getFormulas(): Response<List<XpFormulaDto>>
}