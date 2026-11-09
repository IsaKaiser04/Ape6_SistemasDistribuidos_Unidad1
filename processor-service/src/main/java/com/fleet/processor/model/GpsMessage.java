

/** Servicio Telemetria: consumidor RabbitMQ que procese los mensajes de GPS y los almacene. */
package com.fleet.processor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mensaje GPS tal como lo publica el puente Python (claves en snake_case). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GpsMessage(
        @JsonProperty("vehicle_id") String vehicleId,
        String timestamp,
        Double lat,
        Double lng,
        Double speed) {
}
