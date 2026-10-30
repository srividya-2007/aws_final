package com.ecogrid.prosumer.service;

import com.ecogrid.prosumer.exception.ProsumerNotFoundException;
import com.ecogrid.prosumer.model.ProsumerProfile;
import com.ecogrid.prosumer.repository.ProsumerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProsumerService {

    @Autowired
    private ProsumerProfileRepository profileRepository;

    public ProsumerProfile createProfile(ProsumerProfile profile) {
        if (profileRepository.existsByUsername(profile.getUsername())) {
            throw new IllegalArgumentException("Profile already exists for username: " + profile.getUsername());
        }
        if (profile.getInstalledCapacityKw() == null || profile.getInstalledCapacityKw() <= 0) {
            throw new IllegalArgumentException("Installed capacity must be greater than 0");
        }
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return profileRepository.save(profile);
    }

    public ProsumerProfile getProfileByUsername(String username) {
        return profileRepository.findByUsername(username)
                .orElseThrow(() -> new ProsumerNotFoundException("Prosumer profile not found for: " + username));
    }

    public ProsumerProfile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ProsumerNotFoundException("Prosumer profile not found for id: " + id));
    }

    public List<ProsumerProfile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public ProsumerProfile updateProfile(String username, ProsumerProfile updated) {
        ProsumerProfile existing = getProfileByUsername(username);

        if (updated.getFullName() != null) existing.setFullName(updated.getFullName());
        if (updated.getLocation() != null) existing.setLocation(updated.getLocation());
        if (updated.getInstalledCapacityKw() != null) {
            if (updated.getInstalledCapacityKw() <= 0) {
                throw new IllegalArgumentException("Installed capacity must be greater than 0");
            }
            existing.setInstalledCapacityKw(updated.getInstalledCapacityKw());
        }
        if (updated.getEnergyType() != null) existing.setEnergyType(updated.getEnergyType());
        if (updated.getActive() != null) existing.setActive(updated.getActive());
        existing.setUpdatedAt(LocalDateTime.now());

        return profileRepository.save(existing);
    }

    @Transactional
    public void deleteProfile(String username) {
        if (!profileRepository.existsByUsername(username)) {
            throw new ProsumerNotFoundException("Cannot delete: Profile not found for " + username);
        }
        profileRepository.deleteByUsername(username);
    }
}
