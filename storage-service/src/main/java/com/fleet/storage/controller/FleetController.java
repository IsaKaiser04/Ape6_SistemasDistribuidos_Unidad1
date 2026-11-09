/* REST: Permite consultar el estado de la flota, expone endpoints de resuemn general de la 
flota, consulta la telemetria individual de cada vehiculo */
package com.fleet.storage.controller;

import com.fleet.storage.model.AlertRecord;
import com.fleet.storage.model.GpsRecord;
import com.fleet.storage.repository.AlertRecordRepository;
import com.fleet.storage.repository.GpsRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fleet")
public class FleetController {

    private final GpsRecordRepository gpsRepository;
    private final AlertRecordRepository alertRepository;

    public FleetController(GpsRecordRepository gpsRepository, AlertRecordRepository alertRepository) {
        this.gpsRepository = gpsRepository;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalVehicles", gpsRepository.countDistinctVehicles());
        status.put("gpsRecords", gpsRepository.count());
        status.put("activeAlerts", alertRepository.count());
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/vehicle/{id}/telemetria")
    public ResponseEntity<List<GpsRecord>> getTelemetria(@PathVariable String id) {
        return ResponseEntity.ok(gpsRepository.findTop20ByVehicleIdOrderByIdDesc(id));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRecord>> getAlerts() {
        return ResponseEntity.ok(alertRepository.findTop20ByOrderByIdDesc());
    }
}
