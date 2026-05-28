package com.ace.shared.dto;

/**
 * Métricas agregadas de un bloque de 300 segundos.
 * En el mínimo entregable solo FC promedio es obligatorio.
 */
public record MetricsDto(
    double averageHeartRate,
    Integer maxHeartRate,
    Integer minHeartRate,
    Integer stepCount,
    Double distanceMeters,
    Double caloriesEstimate
) {}
