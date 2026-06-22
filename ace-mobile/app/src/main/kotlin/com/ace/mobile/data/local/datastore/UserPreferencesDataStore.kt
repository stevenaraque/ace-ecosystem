package com.ace.mobile.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ace_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        val CITY_ID = stringPreferencesKey("city_id")
        val STREAK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("streak_notifications_enabled")
        val SYNC_ERROR_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sync_error_notifications_enabled")
        val SESSION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("session_notifications_enabled")
    }

    val cityId: Flow<String?> = dataStore.data.map { it[CITY_ID] }

    val streakNotificationsEnabled: Flow<Boolean> = dataStore.data.map {
        it[STREAK_NOTIFICATIONS_ENABLED] ?: true
    }

    val syncErrorNotificationsEnabled: Flow<Boolean> = dataStore.data.map {
        it[SYNC_ERROR_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setCityId(cityId: String) {
        dataStore.edit { it[CITY_ID] = cityId }
    }

    suspend fun setStreakNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[STREAK_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSyncErrorNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[SYNC_ERROR_NOTIFICATIONS_ENABLED] = enabled }
    }
}