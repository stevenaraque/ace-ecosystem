package sena.adso.ace_backend.exercise.controller

import com.ace.shared.constants.SyncConstants
import com.ace.shared.dto.SyncBatchRequestDto
import com.ace.shared.dto.SyncBatchResponseDto
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import sena.adso.ace_backend.exercise.service.SyncBatchService
import java.util.UUID

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/exercise")
class ExerciseController(
    private val syncBatchService: SyncBatchService
) {

    /**
     * POST /api/exercise/batch
     * Recibe un batch de bloques de ejercicio del móvil.
     * Requiere JWT válido en header Authorization: Bearer <token>
     * 
     * v1.0.5: Ahora recibe sessionId, sentAt, schemaVersion en el body.
     */
    @PostMapping("/batch")
    fun syncBatch(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: SyncBatchRequestDto
    ): ResponseEntity<SyncBatchResponseDto> {
        
        val userId = UUID.fromString(userDetails.username)
        logger.info { 
            "Sync batch requested by user: $userId, " +
            "device: ${request.deviceId}, " +
            "session: ${request.sessionId ?: "not provided"}, " +
            "schemaVersion: ${request.schemaVersion}, " +
            "blocks: ${request.blocks.size}" 
        }

        // Validación básica del batch
        if (request.blocks.isEmpty()) {
            logger.warn { "Empty batch received from user $userId" }
            return ResponseEntity.badRequest().build()
        }

        if (request.blocks.size > SyncConstants.BATCH_MAX_SIZE) {
            logger.warn { "Batch too large: ${request.blocks.size} blocks from user $userId" }
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build()
        }

        // v1.0.5: Validar schemaVersion (futuro: rechazar si es incompatible)
        if (request.schemaVersion != SyncConstants.CURRENT_SCHEMA_VERSION) {
            logger.warn { 
                "Schema version mismatch: client=${request.schemaVersion}, " +
                "server=${SyncConstants.CURRENT_SCHEMA_VERSION}" 
            }
            // Por ahora: aceptar pero loggear (futuro: rechazar versiones muy viejas)
        }

        // v1.0.5: Validar que userId del JWT coincida con userId de los bloques
        val blockUserIds = request.blocks.map { it.userId }.toSet()
        if (blockUserIds.size > 1 || (blockUserIds.isNotEmpty() && blockUserIds.first() != userId.toString())) {
            logger.warn { "UserId mismatch: JWT=$userId, blocks=$blockUserIds" }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return try {
            val response = syncBatchService.processBatch(request, userId)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error(e) { "Failed to process batch for user $userId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}