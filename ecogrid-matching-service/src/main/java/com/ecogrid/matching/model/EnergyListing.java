package com.ecogrid.matching.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "energy_listings")
public class EnergyListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "available_energy_kwh", nullable = false)
    private Double availableEnergyKwh;

    @Column(name = "price_per_kwh", nullable = false)
    private Double pricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ListingStatus status = ListingStatus.AVAILABLE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public EnergyListing() {}

    public EnergyListing(String sellerId, Double availableEnergyKwh, Double pricePerKwh) {
        this.sellerId = sellerId;
        this.availableEnergyKwh = availableEnergyKwh;
        this.pricePerKwh = pricePerKwh;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public Double getAvailableEnergyKwh() { return availableEnergyKwh; }
    public void setAvailableEnergyKwh(Double availableEnergyKwh) { this.availableEnergyKwh = availableEnergyKwh; }

    public Double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(Double pricePerKwh) { this.pricePerKwh = pricePerKwh; }

    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
