package com.ecogrid.prosumer.controller;

import com.ecogrid.prosumer.model.ProsumerProfile;
import com.ecogrid.prosumer.service.ProsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/prosumers")
public class ProsumerController {

    @Autowired
    private ProsumerService prosumerService;

    @PostMapping
    public ResponseEntity<ProsumerProfile> createProfile(@Valid @RequestBody ProsumerProfile profile) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prosumerService.createProfile(profile));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProsumerProfile> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(prosumerService.getProfileByUsername(username));
    }

    @GetMapping
    public ResponseEntity<List<ProsumerProfile>> getAllProfiles() {
        return ResponseEntity.ok(prosumerService.getAllProfiles());
    }

    @PutMapping("/{username}")
    public ResponseEntity<ProsumerProfile> updateProfile(
            @PathVariable String username,
            @RequestBody ProsumerProfile updates) {
        return ResponseEntity.ok(prosumerService.updateProfile(username, updates));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String username) {
        prosumerService.deleteProfile(username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Prosumer Service is UP");
    }
}
