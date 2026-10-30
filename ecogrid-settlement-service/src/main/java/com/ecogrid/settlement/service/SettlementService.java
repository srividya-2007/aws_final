package com.ecogrid.settlement.service;

import com.ecogrid.settlement.model.Settlement;
import com.ecogrid.settlement.model.SettlementStatus;
import com.ecogrid.settlement.repository.SettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SettlementService {

    @Autowired
    private SettlementRepository settlementRepository;

    public Settlement createSettlement(Settlement settlement) {
        // Validate amount
        if (settlement.getEnergyKwh() <= 0) {
            throw new IllegalArgumentException("Energy kWh must be greater than 0");
        }
        if (settlement.getPricePerKwh() <= 0) {
            throw new IllegalArgumentException("Price per kWh must be greater than 0");
        }

        // Prevent duplicate settlement
        if (settlementRepository.existsByOrderId(settlement.getOrderId())) {
            throw new IllegalArgumentException("Settlement already exists for order: " + settlement.getOrderId());
        }

        // Calculate total amount
        settlement.setTotalAmount(settlement.getEnergyKwh() * settlement.getPricePerKwh());
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setCreatedAt(LocalDateTime.now());

        return settlementRepository.save(settlement);
    }

    public Settlement completeSettlement(Long settlementId) {
        Settlement settlement = getSettlementById(settlementId);

        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("Can only complete PENDING settlements. Current status: " + settlement.getStatus());
        }

        String txHash = generateTransactionHash(settlement);
        settlement.setTransactionHash(txHash);
        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setSettledAt(LocalDateTime.now());

        return settlementRepository.save(settlement);
    }

    public Settlement failSettlement(Long settlementId, String reason) {
        Settlement settlement = getSettlementById(settlementId);

        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("Can only fail PENDING settlements");
        }

        settlement.setStatus(SettlementStatus.FAILED);
        return settlementRepository.save(settlement);
    }

    public Settlement getSettlementById(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Settlement not found: " + id));
    }

    public Settlement getSettlementByOrderId(Long orderId) {
        return settlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Settlement not found for order: " + orderId));
    }

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    public List<Settlement> getSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }

    public String generateTransactionHash(Settlement settlement) {
        try {
            String data = settlement.getOrderId() + "-" + settlement.getBuyerId() + "-"
                    + settlement.getSellerId() + "-" + settlement.getTotalAmount()
                    + "-" + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate transaction hash", e);
        }
    }

    public double calculateSettlementAmount(double energyKwh, double pricePerKwh) {
        if (energyKwh <= 0) throw new IllegalArgumentException("Energy kWh must be positive");
        if (pricePerKwh <= 0) throw new IllegalArgumentException("Price per kWh must be positive");
        return energyKwh * pricePerKwh;
    }
}
