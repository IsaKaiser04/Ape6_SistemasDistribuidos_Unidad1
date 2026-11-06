package com.fleet.monitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mensaje GPS tal como lo publica el simulador (claves en snake_case). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GpsMessage(
        @JsonProperty("vehicle_id") String vehicleId,
        String timestamp,
        double lat,
        double lng,
        double speed) {
}
