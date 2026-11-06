package com.fleet.monitor.controller;

import com.fleet.monitor.model.AlertRecord;
import com.fleet.monitor.model.GpsRecord;
import com.fleet.monitor.repository.AlertRecordRepository;
import com.fleet.monitor.repository.GpsRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
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
        Map<String, Object> status = new HashMap<>();
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
