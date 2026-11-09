package com.fleet.processor.model;

/** Alerta normalizada: se envia a storage-service y a cola.notificaciones. */
public record AlertEvent(
        String vehicleId,
        String type,
        String message,
        Double value,
        String timestamp,
        String processedAt) {
}
