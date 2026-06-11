package com.ace.mobile.data.repository

import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalUserEntity
import com.ace.mobile.data.remote.api.AuthApi
import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao
) {

    suspend fun login(email: String, password: String, deviceId: String): Result<AuthResponseDto> {
        return try {
            val response = authApi.login(
                AuthRequestDto(
                    email = email,
                    password = password,
                    deviceId = deviceId
                )
            )

            if (response.isSuccessful) {
                val body = response.body()!!
                val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1000L)
                userDao.insert(
                    LocalUserEntity(
                        userId = body.userId,
                        email = email,
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        tokenExpiresAt = expiresAt,
                        deviceId = deviceId
                    )
                )
                Result.success(body)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, deviceId: String): Result<AuthResponseDto> {
        return try {
            val response = authApi.register(
                AuthRequestDto(
                    email = email,
                    password = password,
                    deviceId = deviceId
                )
            )

            if (response.isSuccessful) {
                val body = response.body()!!
                val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1000L)
                userDao.insert(
                    LocalUserEntity(
                        userId = body.userId,
                        email = email,
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        tokenExpiresAt = expiresAt,
                        deviceId = deviceId
                    )
                )
                Result.success(body)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            userDao.getCurrentUser()?.refreshToken?.let { token ->
                authApi.logout(token)
            }
            userDao.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            userDao.clearUser()
            Result.success(Unit)
        }
    }

    fun isLoggedIn(): Flow<Boolean> = userDao.observeCurrentUser().map { it != null }

    suspend fun getCurrentUserId(): String? = userDao.getCurrentUser()?.userId
}