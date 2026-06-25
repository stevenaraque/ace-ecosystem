package com.ace.mobile.feature.profile.data

import com.ace.shared.dto.UpdateProfileRequestDto
import com.ace.shared.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApi {

    @GET("api/profile")
    suspend fun getProfile(): Response<UserProfileDto>

    @PUT("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<UserProfileDto>
}