package com.ecogrid.matching.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "energy_orders")
public class EnergyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @Column(name = "requested_energy_kwh", nullable = false)
    private Double requestedEnergyKwh;

    @Column(name = "max_price_per_kwh", nullable = false)
    private Double maxPricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "matched_listing_id")
    private Long matchedListingId;

    @Column(name = "matched_energy_kwh")
    private Double matchedEnergyKwh;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public EnergyOrder() {}

    public EnergyOrder(String buyerId, Double requestedEnergyKwh, Double maxPricePerKwh) {
        this.buyerId = buyerId;
        this.requestedEnergyKwh = requestedEnergyKwh;
        this.maxPricePerKwh = maxPricePerKwh;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public Double getRequestedEnergyKwh() { return requestedEnergyKwh; }
    public void setRequestedEnergyKwh(Double requestedEnergyKwh) { this.requestedEnergyKwh = requestedEnergyKwh; }

    public Double getMaxPricePerKwh() { return maxPricePerKwh; }
    public void setMaxPricePerKwh(Double maxPricePerKwh) { this.maxPricePerKwh = maxPricePerKwh; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Long getMatchedListingId() { return matchedListingId; }
    public void setMatchedListingId(Long matchedListingId) { this.matchedListingId = matchedListingId; }

    public Double getMatchedEnergyKwh() { return matchedEnergyKwh; }
    public void setMatchedEnergyKwh(Double matchedEnergyKwh) { this.matchedEnergyKwh = matchedEnergyKwh; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
