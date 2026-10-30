package com.ecogrid.telemetry.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_data")
public class TelemetryData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Prosumer ID is required")
    @Column(name = "prosumer_id", nullable = false)
    private String prosumerId;

    @NotNull(message = "Energy generated cannot be null")
    @PositiveOrZero(message = "Energy generated cannot be negative")
    @Column(name = "energy_generated_kwh", nullable = false)
    private Double energyGeneratedKwh;

    @NotNull(message = "Energy consumed cannot be null")
    @PositiveOrZero(message = "Energy consumed cannot be negative")
    @Column(name = "energy_consumed_kwh", nullable = false)
    private Double energyConsumedKwh;

    @Column(name = "surplus_energy_kwh")
    private Double surplusEnergyKwh;

    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "source_type")
    private String sourceType = "SOLAR";

    public TelemetryData() {}

    public TelemetryData(String prosumerId, double energyGeneratedKwh, double energyConsumedKwh) {
        this.prosumerId = prosumerId;
        this.energyGeneratedKwh = energyGeneratedKwh;
        this.energyConsumedKwh = energyConsumedKwh;
        this.surplusEnergyKwh = energyGeneratedKwh - energyConsumedKwh;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProsumerId() { return prosumerId; }
    public void setProsumerId(String prosumerId) { this.prosumerId = prosumerId; }

    public Double getEnergyGeneratedKwh() { return energyGeneratedKwh; }
    public void setEnergyGeneratedKwh(Double energyGeneratedKwh) { this.energyGeneratedKwh = energyGeneratedKwh; }

    public Double getEnergyConsumedKwh() { return energyConsumedKwh; }
    public void setEnergyConsumedKwh(Double energyConsumedKwh) { this.energyConsumedKwh = energyConsumedKwh; }

    public Double getSurplusEnergyKwh() { return surplusEnergyKwh; }
    public void setSurplusEnergyKwh(Double surplusEnergyKwh) { this.surplusEnergyKwh = surplusEnergyKwh; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
