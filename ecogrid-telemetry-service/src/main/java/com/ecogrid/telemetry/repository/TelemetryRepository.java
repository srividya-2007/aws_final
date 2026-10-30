package com.ecogrid.telemetry.repository;

import com.ecogrid.telemetry.model.TelemetryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryData, Long> {

    List<TelemetryData> findByProsumerIdOrderByTimestampDesc(String prosumerId);

    @Query("SELECT t FROM TelemetryData t WHERE t.prosumerId = :prosumerId ORDER BY t.timestamp DESC LIMIT 1")
    Optional<TelemetryData> findLatestByProsumerId(String prosumerId);

    List<TelemetryData> findBySurplusEnergyKwhGreaterThan(Double threshold);
}
