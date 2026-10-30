package com.ecogrid.telemetry.controller;

import com.ecogrid.telemetry.model.TelemetryData;
import com.ecogrid.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    @Autowired
    private TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<TelemetryData> saveTelemetry(@Valid @RequestBody TelemetryData data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.saveTelemetry(data));
    }

    @GetMapping("/prosumer/{prosumerId}")
    public ResponseEntity<List<TelemetryData>> getTelemetryByProsumer(@PathVariable String prosumerId) {
        return ResponseEntity.ok(telemetryService.getTelemetryByProsumer(prosumerId));
    }

    @GetMapping("/prosumer/{prosumerId}/latest")
    public ResponseEntity<TelemetryData> getLatestTelemetry(@PathVariable String prosumerId) {
        Optional<TelemetryData> latest = telemetryService.getLatestTelemetry(prosumerId);
        return latest.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TelemetryData> getTelemetryById(@PathVariable Long id) {
        return ResponseEntity.ok(telemetryService.getTelemetryById(id));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Telemetry Service is UP");
    }
}
