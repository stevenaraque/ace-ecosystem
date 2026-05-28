package com.ace.shared.dto;

import com.ace.shared.enums.SportType;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO inmutable que representa un bloque de ejercicio de 300 segundos.
 * Contrato sagrado entre Mobile y Backend. Cualquier cambio requiere
 * publicar nueva versión del JAR y notificar a todos los equipos.
 */
public record ExerciseBlockDto(
    UUID blockId,
    UUID sessionId,
    UUID userId,
    String deviceId,
    SportType sportType,
    Instant timestampStart,
    Instant timestampEnd,
    MetricsDto metrics,
    int schemaVersion
) {
    public ExerciseBlockDto {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
    }
}
