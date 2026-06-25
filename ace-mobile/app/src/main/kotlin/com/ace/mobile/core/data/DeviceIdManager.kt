package com.ace.mobile.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: generateAndSave()

    private fun generateAndSave(): String {
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    companion object {
        private const val PREFS_NAME = "ace_secure_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}