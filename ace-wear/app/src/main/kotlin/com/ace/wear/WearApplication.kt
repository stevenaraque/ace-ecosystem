// ace-wear/app/src/main/kotlin/com/ace/wear/WearApplication.kt

package com.ace.wear

import android.app.Application
import android.util.Log
import com.ace.wear.presentation.session.SessionViewModel
import dagger.hilt.android.HiltAndroidApp

/**
 * Aplicacion principal del reloj Wear OS.
 *
 * Responsabilidades:
 * - Inicializar Hilt (inyeccion de dependencias)
 * - Crear NotificationChannels si fueran necesarios (S8, reservado para MVP)
 *
 * La inicializacion del repositorio de salud (WearHealthRepository) ocurre en
 * SessionViewModel.initialize() llamado desde MainActivity.onCreate().
 *
 * @see Apendice S1 §3.3 (Reloj no persiste, no decide, solo reacciona)
 */
@HiltAndroidApp
class WearApplication : Application() {

    companion object {
        private const val TAG = "WearApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearApplication iniciada")
    }
}