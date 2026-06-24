package com.ace.mobile.data.remote.interceptor

import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalUserEntity
import com.ace.mobile.data.remote.api.AuthApi
import com.ace.shared.dto.RefreshTokenRequestDto
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Provider // Importación necesaria para romper la dependencia cíclica
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val userDao: UserDao,
    private val authApiProvider: Provider<AuthApi> // Rompe el bucle de inyección Hilt -> OkHttp -> Retrofit
) : Interceptor {

    private val lock = Any()
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = runBlocking { userDao.getCurrentUser() }
        val token = user?.accessToken

        val authenticatedRequest = if (token != null && shouldAddAuth(request)) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        // Interceptamos el 401 y validamos estrictamente el cuerpo "TOKEN_EXPIRED" del backend
        if (response.code == 401 && isTokenExpired(response)) {
            response.close() // Cerramos la respuesta anterior para evitar memory leaks

            val newToken = synchronized(lock) {
                runBlocking {
                    val freshUser = userDao.getCurrentUser()
                    // Si otra petición en paralelo ya completó el refresh con éxito, tomamos ese token directamente
                    if (freshUser?.accessToken != token && freshUser?.accessToken != null) {
                        freshUser.accessToken
                    } else {
                        performRefresh(freshUser)
                    }
                }
            }

            return if (newToken != null) {
                // Reintentamos la petición original mutando el Header de autorización
                val retryRequest = request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // Si el refresh no devolvió un token, la sesión expiró por completo
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("".toResponseBody(null))
                    .build()
            }
        }

        return response
    }

    private fun isTokenExpired(response: Response): Boolean {
        return try {
            val bodyString = response.peekBody(Long.MAX_VALUE).string()
            val errorObj = gson.fromJson(bodyString, JsonObject::class.java)
            val error = errorObj.get("error")?.asString
            // FIX: El backend devuelve {"error":"INVALID_TOKEN"} cuando el token está
            // expirado (la rama TOKEN_EXPIRED del JwtAuthenticationFilter es inalcanzable
            // porque validateAccessToken captura ExpiredJwtException y devuelve null).
            // Aceptamos ambos códigos para que el refresh se dispare en cualquier caso.
            error == "TOKEN_EXPIRED" || error == "INVALID_TOKEN"
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun performRefresh(user: LocalUserEntity?): String? {
        val refreshToken = user?.refreshToken ?: return null
        val deviceId = user.deviceId

        return try {
            // Obtenemos la instancia de AuthApi dinámicamente mediante el Provider
            val retrofitResponse = authApiProvider.get().refresh(
                RefreshTokenRequestDto(
                    refreshToken = refreshToken,
                    deviceId = deviceId
                )
            )

            if (retrofitResponse.isSuccessful) {
                val body = retrofitResponse.body()!!
                val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1000L)
                userDao.updateAccessToken(user.userId, body.accessToken, expiresAt)
                userDao.updateRefreshToken(user.userId, body.refreshToken)
                body.accessToken
            } else {
                // Si el backend rechaza el Refresh Token (401), se asume sesión robada o vencida permanentemente
                if (retrofitResponse.code() == 401) {
                    // Limpieza lógica: Mantenemos el deviceId para preservar el ciclo de auditoría del terminal
                    userDao.clearTokens(user.userId)
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun shouldAddAuth(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        return !path.contains("/auth/login") &&
                !path.contains("/auth/register") &&
                !path.contains("/auth/refresh") &&
                !path.contains("/auth/logout") // Excluido para que no intente autorefrescarse durante la desconexión
    }
}