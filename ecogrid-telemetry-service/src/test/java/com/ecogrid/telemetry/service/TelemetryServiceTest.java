package com.ecogrid.telemetry.service;

import com.ecogrid.telemetry.model.TelemetryData;
import com.ecogrid.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Telemetry Service Unit Tests")
class TelemetryServiceTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    private TelemetryData sampleTelemetry;

    @BeforeEach
    void setUp() {
        sampleTelemetry = new TelemetryData("prosumer-001", 10.0, 4.0);
        sampleTelemetry.setId(1L);
        sampleTelemetry.setTimestamp(LocalDateTime.now());
    }

    // ─── SURPLUS ENERGY CALCULATION TESTS ────────────────────────────────────

    @Test
    @DisplayName("Should calculate surplus energy correctly: 10.0 - 4.0 = 6.0")
    void shouldCalculateSurplusEnergyCorrectly() {
        // Arrange
        double generation = 10.0;
        double consumption = 4.0;

        // Act
        double surplus = telemetryService.calculateSurplusEnergy(generation, consumption);

        // Assert
        assertThat(surplus).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Should calculate zero surplus when generation equals consumption")
    void shouldCalculateZeroSurplusWhenBalanced() {
        // Act
        double surplus = telemetryService.calculateSurplusEnergy(5.0, 5.0);

        // Assert
        assertThat(surplus).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate negative surplus (deficit) when consumption exceeds generation")
    void shouldCalculateNegativeSurplusWhenConsumptionExceedsGeneration() {
        // Act
        double surplus = telemetryService.calculateSurplusEnergy(3.0, 7.0);

        // Assert
        assertThat(surplus).isEqualTo(-4.0);
    }

    @Test
    @DisplayName("Should reject negative energy generated value")
    void shouldRejectNegativeEnergyGenerated() {
        // Act & Assert
        assertThatThrownBy(() -> telemetryService.calculateSurplusEnergy(-1.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Generated energy cannot be negative");
    }

    @Test
    @DisplayName("Should reject negative energy consumed value")
    void shouldRejectNegativeEnergyConsumed() {
        // Act & Assert
        assertThatThrownBy(() -> telemetryService.calculateSurplusEnergy(5.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consumed energy cannot be negative");
    }

    // ─── SAVE TELEMETRY TESTS ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should save telemetry data successfully and compute surplus")
    void shouldSaveTelemetryAndComputeSurplus() {
        // Arrange
        TelemetryData input = new TelemetryData();
        input.setProsumerId("prosumer-001");
        input.setEnergyGeneratedKwh(10.0);
        input.setEnergyConsumedKwh(4.0);

        TelemetryData savedData = new TelemetryData("prosumer-001", 10.0, 4.0);
        savedData.setId(1L);
        savedData.setSurplusEnergyKwh(6.0);

        when(telemetryRepository.save(any(TelemetryData.class))).thenReturn(savedData);

        // Act
        TelemetryData result = telemetryService.saveTelemetry(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSurplusEnergyKwh()).isEqualTo(6.0);
        verify(telemetryRepository).save(any(TelemetryData.class));
    }

    @Test
    @DisplayName("Should reject save when energy generated is negative")
    void shouldRejectSaveWithNegativeEnergyGenerated() {
        // Arrange
        TelemetryData invalid = new TelemetryData();
        invalid.setProsumerId("prosumer-002");
        invalid.setEnergyGeneratedKwh(-5.0);
        invalid.setEnergyConsumedKwh(3.0);

        // Act & Assert
        assertThatThrownBy(() -> telemetryService.saveTelemetry(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Energy generated cannot be negative");

        verify(telemetryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject save when energy consumed is negative")
    void shouldRejectSaveWithNegativeEnergyConsumed() {
        // Arrange
        TelemetryData invalid = new TelemetryData();
        invalid.setProsumerId("prosumer-003");
        invalid.setEnergyGeneratedKwh(5.0);
        invalid.setEnergyConsumedKwh(-2.0);

        // Act & Assert
        assertThatThrownBy(() -> telemetryService.saveTelemetry(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Energy consumed cannot be negative");
    }

    // ─── RETRIEVE TELEMETRY TESTS ─────────────────────────────────────────────

    @Test
    @DisplayName("Should retrieve telemetry records for a prosumer")
    void shouldRetrieveTelemetryByProsumer() {
        // Arrange
        TelemetryData record2 = new TelemetryData("prosumer-001", 8.0, 3.0);
        record2.setId(2L);
        when(telemetryRepository.findByProsumerIdOrderByTimestampDesc("prosumer-001"))
                .thenReturn(List.of(sampleTelemetry, record2));

        // Act
        List<TelemetryData> result = telemetryService.getTelemetryByProsumer("prosumer-001");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TelemetryData::getProsumerId)
                .containsOnly("prosumer-001");
    }

    @Test
    @DisplayName("Should retrieve latest telemetry for a prosumer")
    void shouldRetrieveLatestTelemetry() {
        // Arrange
        when(telemetryRepository.findLatestByProsumerId("prosumer-001"))
                .thenReturn(Optional.of(sampleTelemetry));

        // Act
        Optional<TelemetryData> latest = telemetryService.getLatestTelemetry("prosumer-001");

        // Assert
        assertThat(latest).isPresent();
        assertThat(latest.get().getProsumerId()).isEqualTo("prosumer-001");
        assertThat(latest.get().getSurplusEnergyKwh()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("Should return empty optional when no telemetry exists")
    void shouldReturnEmptyWhenNoTelemetryExists() {
        // Arrange
        when(telemetryRepository.findLatestByProsumerId("unknown")).thenReturn(Optional.empty());

        // Act
        Optional<TelemetryData> result = telemetryService.getLatestTelemetry("unknown");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when telemetry record not found by ID")
    void shouldThrowWhenTelemetryNotFoundById() {
        // Arrange
        when(telemetryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> telemetryService.getTelemetryById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Telemetry record not found");
    }

    // ─── SURPLUS ENERGY VALIDATION ACROSS SCENARIOS ───────────────────────────

    @Test
    @DisplayName("Should correctly compute surplus for large energy values")
    void shouldComputeSurplusForLargeValues() {
        // Act
        double surplus = telemetryService.calculateSurplusEnergy(500.0, 350.75);

        // Assert
        assertThat(surplus).isCloseTo(149.25, within(0.001));
    }

    @Test
    @DisplayName("Should compute zero surplus when no generation occurs")
    void shouldComputeZeroSurplusWithNoGeneration() {
        // Act
        double surplus = telemetryService.calculateSurplusEnergy(0.0, 0.0);

        // Assert
        assertThat(surplus).isEqualTo(0.0);
    }
}
