package com.ecogrid.prosumer.repository;

import com.ecogrid.prosumer.model.ProsumerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProsumerProfileRepository extends JpaRepository<ProsumerProfile, Long> {
    Optional<ProsumerProfile> findByUsername(String username);
    boolean existsByUsername(String username);
    void deleteByUsername(String username);
}
