package com.ace.mobile.feature.profile.data

import com.ace.mobile.core.datastore.UserPreferencesDataStore
import com.ace.mobile.core.database.dao.UserDao
import com.ace.shared.dto.UpdateProfileRequestDto
import com.ace.shared.dto.UserProfileDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {

    /**
     * Obtiene el perfil del usuario autenticado.
     * Primero intenta la red; si falla, devuelve null (no hay cache local de perfil aún).
     */
    suspend fun getProfile(): Result<UserProfileDto> {
        return try {
            val response = userApi.getProfile()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Sincronizar cityId en DataStore si viene del backend
                    body.cityId?.let { cityId ->
                        userPreferencesDataStore.setCityId(cityId)
                    }
                    Result.success(body)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el perfil del usuario.
     * Tras guardar exitosamente, persiste cityId en DataStore si cambió.
     */
    suspend fun updateProfile(request: UpdateProfileRequestDto): Result<UserProfileDto> {
        return try {
            val response = userApi.updateProfile(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // CRÍTICO: Persistir cityId en DataStore para desbloquear ranking municipal
                    body.cityId?.let { cityId ->
                        userPreferencesDataStore.setCityId(cityId)
                    }
                    Result.success(body)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el cityId almacenado localmente (para mostrar en UI antes de cargar perfil).
     */
    suspend fun getCachedCityId(): String? {
        return userPreferencesDataStore.cityId.first()
    }
}