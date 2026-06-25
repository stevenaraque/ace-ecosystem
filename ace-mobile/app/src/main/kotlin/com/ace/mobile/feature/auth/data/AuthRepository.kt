package com.ace.mobile.feature.auth.data

import com.ace.mobile.core.database.dao.UserDao
import com.ace.mobile.core.database.entity.LocalUserEntity
import com.ace.mobile.feature.auth.data.AuthApi
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
                // FIX: Limpiar cualquier usuario anterior del dispositivo antes de insertar.
                // Sin esto, cada login con un userId distinto añade una fila nueva a
                // local_user y getCurrentUser() podía devolver la equivocada.
                userDao.clearUser()
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
                // FIX: Limpiar cualquier usuario anterior del dispositivo antes de insertar.
                userDao.clearUser()
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
            val currentUser = userDao.getCurrentUser()

            if (currentUser != null) {
                // 1. Intento de revocación remota en Backend (Fire-and-forget)
                // Si falla la red, el catch interno absorbe el error para no bloquear el flujo local.
                currentUser.refreshToken?.let { token ->
                    runCatching {
                        authApi.logout(token)
                    }
                }

                // 2. Limpieza local: eliminamos la fila del usuario.
                // FIX: Antes se usaba clearTokens() que solo anulaba accessToken/refreshToken
                // pero mantenía la fila viva, lo que provocaba sesiones residuales y userIds
                // stale. Ahora clearUser() elimina la fila por completo.
                userDao.clearUser()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            // Si algo falla a nivel base de datos local, propagamos la excepción
            Result.failure(e)
        }
    }

    /**
     * Ajustado para verificar la existencia del token de acceso.
     * Como clearTokens() mantiene la fila del usuario viva en SQLite,
     * evaluar solo con 'it != null' provocaría falsos positivos de sesión activa.
     */
    fun isLoggedIn(): Flow<Boolean> = userDao.observeCurrentUser().map { user ->
        user != null && !user.accessToken.isNullOrEmpty()
    }

    suspend fun getCurrentUserId(): String? = userDao.getCurrentUser()?.userId
}