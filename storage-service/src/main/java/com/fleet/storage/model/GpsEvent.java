package com.fleet.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Evento que envia el processor (mismo JSON, clase propia de este servicio). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GpsEvent(
        String vehicleId,
        String timestamp,
        double lat,
        double lng,
        double speed,
        String processedAt) {
}
