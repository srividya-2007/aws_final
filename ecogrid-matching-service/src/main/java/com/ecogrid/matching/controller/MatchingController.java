package com.ecogrid.matching.controller;

import com.ecogrid.matching.model.EnergyListing;
import com.ecogrid.matching.model.EnergyOrder;
import com.ecogrid.matching.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    @Autowired
    private MatchingService matchingService;

    @PostMapping("/listings")
    public ResponseEntity<EnergyListing> createListing(@RequestBody EnergyListing listing) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchingService.createListing(listing));
    }

    @GetMapping("/listings")
    public ResponseEntity<List<EnergyListing>> getAvailableListings() {
        return ResponseEntity.ok(matchingService.getAvailableListings());
    }

    @PostMapping("/orders")
    public ResponseEntity<EnergyOrder> placeOrder(@RequestBody EnergyOrder order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchingService.placeOrder(order));
    }

    @PostMapping("/orders/{orderId}/match")
    public ResponseEntity<EnergyOrder> matchOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(matchingService.matchOrder(orderId));
    }

    @GetMapping("/orders/buyer/{buyerId}")
    public ResponseEntity<List<EnergyOrder>> getOrdersByBuyer(@PathVariable String buyerId) {
        return ResponseEntity.ok(matchingService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Matching Service is UP");
    }
}
