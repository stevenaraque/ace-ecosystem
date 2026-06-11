package com.ace.mobile.data.remote.api

import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import com.ace.shared.dto.RefreshTokenRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequestDto): Response<AuthResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequestDto): Response<AuthResponseDto>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<AuthResponseDto>

    @POST("api/auth/logout")
    suspend fun logout(@Header("X-Refresh-Token") refreshToken: String): Response<Unit>
}