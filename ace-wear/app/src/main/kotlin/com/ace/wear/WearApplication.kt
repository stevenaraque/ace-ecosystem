package com.ace.wear

import android.app.Application
import android.util.Log
import com.ace.wear.data.sync.WearMessageClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Aplicacion principal del reloj Wear OS.
 *
 * Responsabilidades:
 * - Inicializar Hilt (inyeccion de dependencias)
 * - Registrar listener de MessageClient para recibir comandos del movil
 *
 * La inicializacion del repositorio de salud ocurre en
 * SessionViewModel.initialize() llamado desde MainActivity.onCreate().
 */
@HiltAndroidApp
class WearApplication : Application() {

    companion object {
        private const val TAG = "WearApplication"
    }

    @Inject
    lateinit var wearMessageClient: WearMessageClient

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearApplication iniciada")

        // UNICO lugar donde se registra el listener de MessageClient
        wearMessageClient.startListening()
        Log.i(TAG, "WearMessageClient listener registrado")
    }
}