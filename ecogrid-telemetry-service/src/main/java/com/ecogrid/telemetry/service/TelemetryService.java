package com.ecogrid.telemetry.service;

import com.ecogrid.telemetry.model.TelemetryData;
import com.ecogrid.telemetry.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TelemetryService {

    @Autowired
    private TelemetryRepository telemetryRepository;

    public TelemetryData saveTelemetry(TelemetryData data) {
        if (data.getEnergyGeneratedKwh() < 0) {
            throw new IllegalArgumentException("Energy generated cannot be negative");
        }
        if (data.getEnergyConsumedKwh() < 0) {
            throw new IllegalArgumentException("Energy consumed cannot be negative");
        }

        // Calculate surplus
        double surplus = data.getEnergyGeneratedKwh() - data.getEnergyConsumedKwh();
        data.setSurplusEnergyKwh(surplus);

        if (data.getTimestamp() == null) {
            data.setTimestamp(LocalDateTime.now());
        }

        return telemetryRepository.save(data);
    }

    public List<TelemetryData> getTelemetryByProsumer(String prosumerId) {
        return telemetryRepository.findByProsumerIdOrderByTimestampDesc(prosumerId);
    }

    public Optional<TelemetryData> getLatestTelemetry(String prosumerId) {
        return telemetryRepository.findLatestByProsumerId(prosumerId);
    }

    public TelemetryData getTelemetryById(Long id) {
        return telemetryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Telemetry record not found: " + id));
    }

    public double calculateSurplusEnergy(double generated, double consumed) {
        if (generated < 0) throw new IllegalArgumentException("Generated energy cannot be negative");
        if (consumed < 0) throw new IllegalArgumentException("Consumed energy cannot be negative");
        return generated - consumed;
    }

    public List<TelemetryData> getAllTelemetry() {
        return telemetryRepository.findAll();
    }
}
