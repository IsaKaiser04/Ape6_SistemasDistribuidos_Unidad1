package com.fleet.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertEvent(
        String vehicleId,
        String type,
        String message,
        Double value,
        String timestamp,
        String processedAt) {
}
