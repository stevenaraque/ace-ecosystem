package sena.adso.ace_backend.exercise.dto

import com.ace.shared.dto.ExerciseBlockDto
import com.ace.shared.dto.ClientStatsDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * DTO interno del backend para recibir batches.
 * Envuelve el DTO de :shared para agregar validaciones Bean Validation.
 * 
 * NOTA: Si prefieres usar directamente SyncBatchRequestDto de :shared,
 * elimina este archivo y usa @Valid en el controller.
 */
data class BatchRequest(
    @field:NotBlank
    val deviceId: String,

    @field:NotBlank
    val sessionId: String,

    @field:NotEmpty
    @field:Valid
    val blocks: List<ExerciseBlockDto>,

    @field:Valid
    val clientStats: ClientStatsDto?,

    val sentAt: Long
)