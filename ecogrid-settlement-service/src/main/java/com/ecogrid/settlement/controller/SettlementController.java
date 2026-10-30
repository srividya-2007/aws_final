package com.ecogrid.settlement.controller;

import com.ecogrid.settlement.model.Settlement;
import com.ecogrid.settlement.model.SettlementStatus;
import com.ecogrid.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @PostMapping
    public ResponseEntity<Settlement> createSettlement(@RequestBody Settlement settlement) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.createSettlement(settlement));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Settlement> getSettlement(@PathVariable Long id) {
        return ResponseEntity.ok(settlementService.getSettlementById(id));
    }

    @GetMapping
    public ResponseEntity<List<Settlement>> getAllSettlements() {
        return ResponseEntity.ok(settlementService.getAllSettlements());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Settlement> completeSettlement(@PathVariable Long id) {
        return ResponseEntity.ok(settlementService.completeSettlement(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Settlement> failSettlement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Processing failed") String reason) {
        return ResponseEntity.ok(settlementService.failSettlement(id, reason));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Settlement Service is UP");
    }
}
