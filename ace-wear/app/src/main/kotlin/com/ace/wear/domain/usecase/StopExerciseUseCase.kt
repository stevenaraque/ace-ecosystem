// ace-wear/app/src/main/kotlin/com/ace/wear/domain/usecase/StopExerciseUseCase.kt

package com.ace.wear.domain.usecase

import com.ace.wear.data.repository.WearHealthRepository
import javax.inject.Inject

/**
 * Caso de uso para detener y limpiar el monitoreo de ejercicio.
 *
 * Delega a WearHealthRepository que:
 * 1. Detiene HealthServicesManager
 * 2. Detiene escucha de mensajes del movil
 * 3. Limpia recursos
 *
 * Debe llamarse al cerrar la app (onDestroy de MainActivity).
 */
class StopExerciseUseCase @Inject constructor(
    private val wearHealthRepository: WearHealthRepository
) {
    /**
     * Libera todos los recursos del repositorio de salud.
     */
    operator fun invoke() {
        wearHealthRepository.dispose()
    }
}