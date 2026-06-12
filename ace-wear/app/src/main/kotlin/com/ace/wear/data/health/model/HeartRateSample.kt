package com.ace.wear.data.health.model

/**
 * Representa una muestra individual de frecuencia cardíaca capturada
 * por Health Services API (HEART_RATE_BPM).
 *
 * Campos extraídos directamente del tipo nativo de Health Services:
 * - [bpm]: valor del campo `value` (Double)
 * - [timestamp]: derivado del campo `time_interval.start` (epoch millis)
 *
 * El reloj no reinterpreta ni transforma estos valores.
 * Los transporta tal cual los generó Health Services.
 */
data class HeartRateSample(
    /** Latidos por minuto. Extraído del campo `value` del tipo nativo HEART_RATE_BPM. */
    val bpm: Double,

    /**
     * Timestamp de captura en epoch millis.
     * Para frecuencia cardíaca instantánea, el `time_interval` del tipo nativo
     * tiene start y end que suelen coincidir o representar una ventana de un segundo.
     * Usamos el start como timestamp único de la muestra.
     */
    val timestamp: Long
)