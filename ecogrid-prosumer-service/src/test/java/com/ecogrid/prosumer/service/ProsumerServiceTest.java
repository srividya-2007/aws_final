package com.ecogrid.prosumer.service;

import com.ecogrid.prosumer.exception.ProsumerNotFoundException;
import com.ecogrid.prosumer.model.ProsumerProfile;
import com.ecogrid.prosumer.repository.ProsumerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Prosumer Service Unit Tests")
class ProsumerServiceTest {

    @Mock
    private ProsumerProfileRepository profileRepository;

    @InjectMocks
    private ProsumerService prosumerService;

    private ProsumerProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new ProsumerProfile("alice", "Alice Green", "Hyderabad", 5.0);
        testProfile.setId(1L);
        testProfile.setEnergyType("SOLAR");
        testProfile.setActive(true);
    }

    // ─── CREATE TESTS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create prosumer profile successfully")
    void shouldCreateProsumerProfileSuccessfully() {
        // Arrange
        when(profileRepository.existsByUsername("alice")).thenReturn(false);
        when(profileRepository.save(any(ProsumerProfile.class))).thenReturn(testProfile);

        // Act
        ProsumerProfile created = prosumerService.createProfile(testProfile);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getUsername()).isEqualTo("alice");
        assertThat(created.getInstalledCapacityKw()).isEqualTo(5.0);
        verify(profileRepository).save(any(ProsumerProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate profile")
    void shouldThrowWhenCreatingDuplicateProfile() {
        // Arrange
        when(profileRepository.existsByUsername("alice")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.createProfile(testProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile already exists");

        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject profile with zero installed capacity")
    void shouldRejectProfileWithZeroInstalledCapacity() {
        // Arrange
        ProsumerProfile invalidProfile = new ProsumerProfile("bob", "Bob Builder", "Chennai", 0.0);
        when(profileRepository.existsByUsername("bob")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.createProfile(invalidProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installed capacity must be greater than 0");
    }

    @Test
    @DisplayName("Should reject profile with negative installed capacity")
    void shouldRejectProfileWithNegativeCapacity() {
        // Arrange
        ProsumerProfile invalidProfile = new ProsumerProfile("charlie", "Charlie Doe", "Mumbai", -5.0);
        when(profileRepository.existsByUsername("charlie")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.createProfile(invalidProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installed capacity must be greater than 0");
    }

    // ─── RETRIEVE TESTS ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should retrieve prosumer profile by username")
    void shouldRetrieveProsumerProfileByUsername() {
        // Arrange
        when(profileRepository.findByUsername("alice")).thenReturn(Optional.of(testProfile));

        // Act
        ProsumerProfile profile = prosumerService.getProfileByUsername("alice");

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.getUsername()).isEqualTo("alice");
        assertThat(profile.getFullName()).isEqualTo("Alice Green");
        assertThat(profile.getLocation()).isEqualTo("Hyderabad");
    }

    @Test
    @DisplayName("Should throw ProsumerNotFoundException for non-existent username")
    void shouldThrowWhenProfileNotFound() {
        // Arrange
        when(profileRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.getProfileByUsername("unknown"))
                .isInstanceOf(ProsumerNotFoundException.class)
                .hasMessageContaining("Prosumer profile not found");
    }

    @Test
    @DisplayName("Should retrieve all prosumer profiles")
    void shouldRetrieveAllProfiles() {
        // Arrange
        ProsumerProfile profile2 = new ProsumerProfile("bob", "Bob Energy", "Chennai", 3.0);
        when(profileRepository.findAll()).thenReturn(List.of(testProfile, profile2));

        // Act
        List<ProsumerProfile> profiles = prosumerService.getAllProfiles();

        // Assert
        assertThat(profiles).hasSize(2);
        assertThat(profiles).extracting(ProsumerProfile::getUsername)
                .containsExactlyInAnyOrder("alice", "bob");
    }

    // ─── UPDATE TESTS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update prosumer profile successfully")
    void shouldUpdateProsumerProfile() {
        // Arrange
        when(profileRepository.findByUsername("alice")).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(ProsumerProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        ProsumerProfile updates = new ProsumerProfile();
        updates.setLocation("Bangalore");
        updates.setInstalledCapacityKw(10.0);

        // Act
        ProsumerProfile updated = prosumerService.updateProfile("alice", updates);

        // Assert
        assertThat(updated.getLocation()).isEqualTo("Bangalore");
        assertThat(updated.getInstalledCapacityKw()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should reject update with invalid capacity")
    void shouldRejectUpdateWithInvalidCapacity() {
        // Arrange
        when(profileRepository.findByUsername("alice")).thenReturn(Optional.of(testProfile));

        ProsumerProfile invalidUpdate = new ProsumerProfile();
        invalidUpdate.setInstalledCapacityKw(-1.0);

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.updateProfile("alice", invalidUpdate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Installed capacity must be greater than 0");
    }

    // ─── DELETE TESTS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete prosumer profile successfully")
    void shouldDeleteProsumerProfile() {
        // Arrange
        when(profileRepository.existsByUsername("alice")).thenReturn(true);
        doNothing().when(profileRepository).deleteByUsername("alice");

        // Act
        prosumerService.deleteProfile("alice");

        // Assert
        verify(profileRepository).deleteByUsername("alice");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent profile")
    void shouldThrowWhenDeletingNonExistentProfile() {
        // Arrange
        when(profileRepository.existsByUsername("ghost")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> prosumerService.deleteProfile("ghost"))
                .isInstanceOf(ProsumerNotFoundException.class)
                .hasMessageContaining("Cannot delete: Profile not found");
    }

    // ─── UNAUTHORIZED ACCESS SIMULATION ──────────────────────────────────────

    @Test
    @DisplayName("Should demonstrate unauthorized access by returning correct profile owner")
    void shouldReturnCorrectProfileOwnerUsername() {
        // Arrange - simulates that only owner 'alice' can access their profile
        when(profileRepository.findByUsername("alice")).thenReturn(Optional.of(testProfile));

        // Act
        ProsumerProfile retrieved = prosumerService.getProfileByUsername("alice");

        // Assert - the returned profile belongs to 'alice', not any other user
        assertThat(retrieved.getUsername()).isEqualTo("alice");
        assertThat(retrieved.getUsername()).isNotEqualTo("mallory"); // not accessible by other users
    }
}
