// feature/profile/presentation/CityConstants.kt
package com.ace.mobile.feature.profile.presentation

data class City(val id: String, val displayName: String)

object CityConstants {
    val CITIES = listOf(
        City("bogota", "Bogotá"),
        City("medellin", "Medellín"),
        City("cali", "Cali"),
        City("barranquilla", "Barranquilla")
    )

    fun getDisplayName(cityId: String?): String {
        return CITIES.find { it.id == cityId }?.displayName ?: "Sin ciudad"
    }

    fun getId(displayName: String): String? {
        return CITIES.find { it.displayName == displayName }?.id
    }
}