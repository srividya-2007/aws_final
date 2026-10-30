package com.ecogrid.prosumer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "prosumer_profiles")
public class ProsumerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Installed capacity is required")
    @Positive(message = "Installed capacity must be positive")
    @Column(name = "installed_capacity_kw")
    private Double installedCapacityKw;

    @Column(name = "energy_type")
    private String energyType = "SOLAR";

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ProsumerProfile() {}

    public ProsumerProfile(String username, String fullName, String location, Double installedCapacityKw) {
        this.username = username;
        this.fullName = fullName;
        this.location = location;
        this.installedCapacityKw = installedCapacityKw;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getInstalledCapacityKw() { return installedCapacityKw; }
    public void setInstalledCapacityKw(Double installedCapacityKw) { this.installedCapacityKw = installedCapacityKw; }

    public String getEnergyType() { return energyType; }
    public void setEnergyType(String energyType) { this.energyType = energyType; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
