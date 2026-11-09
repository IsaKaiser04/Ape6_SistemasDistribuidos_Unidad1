package com.fleet.processor.model;

/** Evento GPS ya validado y normalizado (camelCase) que se envia a storage-service. */
public record GpsEvent(
        String vehicleId,
        String timestamp,
        double lat,
        double lng,
        double speed,
        String processedAt) {
}
