package com.ecogrid.settlement.service;

import com.ecogrid.settlement.model.Settlement;
import com.ecogrid.settlement.model.SettlementStatus;
import com.ecogrid.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Settlement Service Unit Tests")
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementService settlementService;

    private Settlement pendingSettlement;

    @BeforeEach
    void setUp() {
        pendingSettlement = new Settlement(101L, "buyer-001", "seller-001", 6.0, 5.0);
        pendingSettlement.setId(1L);
        pendingSettlement.setTotalAmount(30.0);
        pendingSettlement.setStatus(SettlementStatus.PENDING);
    }

    // ─── CREATE SETTLEMENT TESTS ──────────────────────────────────────────────

    @Test
    @DisplayName("Should create settlement successfully")
    void shouldCreateSettlementSuccessfully() {
        // Arrange
        when(settlementRepository.existsByOrderId(101L)).thenReturn(false);
        when(settlementRepository.save(any(Settlement.class))).thenReturn(pendingSettlement);

        // Act
        Settlement created = settlementService.createSettlement(pendingSettlement);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getOrderId()).isEqualTo(101L);
        assertThat(created.getBuyerId()).isEqualTo("buyer-001");
        assertThat(created.getStatus()).isEqualTo(SettlementStatus.PENDING);
        verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    @DisplayName("Should correctly calculate settlement total amount: 6.0 kWh * 5.0 = 30.0")
    void shouldCalculateSettlementAmountCorrectly() {
        // Arrange
        when(settlementRepository.existsByOrderId(anyLong())).thenReturn(false);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement newSettlement = new Settlement(200L, "buyer-002", "seller-002", 6.0, 5.0);

        // Act
        Settlement created = settlementService.createSettlement(newSettlement);

        // Assert
        assertThat(created.getTotalAmount()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should validate settlement amount calculation directly")
    void shouldValidateAmountCalculationDirectly() {
        // Act
        double amount = settlementService.calculateSettlementAmount(8.0, 4.5);

        // Assert
        assertThat(amount).isCloseTo(36.0, within(0.001));
    }

    @Test
    @DisplayName("Should reject settlement with zero energy kWh")
    void shouldRejectSettlementWithZeroEnergy() {
        // Arrange
        Settlement invalid = new Settlement(102L, "buyer", "seller", 0.0, 5.0);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.createSettlement(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Energy kWh must be greater than 0");
    }

    @Test
    @DisplayName("Should reject settlement with zero price per kWh")
    void shouldRejectSettlementWithZeroPrice() {
        // Arrange
        Settlement invalid = new Settlement(103L, "buyer", "seller", 5.0, 0.0);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.createSettlement(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price per kWh must be greater than 0");
    }

    @Test
    @DisplayName("Should prevent duplicate settlement for same order")
    void shouldPreventDuplicateSettlement() {
        // Arrange
        when(settlementRepository.existsByOrderId(101L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> settlementService.createSettlement(pendingSettlement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Settlement already exists for order");

        verify(settlementRepository, never()).save(any());
    }

    // ─── COMPLETE SETTLEMENT TESTS ────────────────────────────────────────────

    @Test
    @DisplayName("Should complete a pending settlement with transaction hash")
    void shouldCompleteSettlementWithTransactionHash() {
        // Arrange
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(pendingSettlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Settlement completed = settlementService.completeSettlement(1L);

        // Assert
        assertThat(completed.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(completed.getTransactionHash()).isNotNull();
        assertThat(completed.getTransactionHash()).isNotBlank();
        assertThat(completed.getSettledAt()).isNotNull();
    }

    @Test
    @DisplayName("Should generate a valid SHA-256 transaction hash")
    void shouldGenerateValidTransactionHash() {
        // Act
        String hash = settlementService.generateTransactionHash(pendingSettlement);

        // Assert
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SHA-256 produces 64 hex chars
        assertThat(hash).matches("[0-9a-f]+"); // hex characters only
    }

    @Test
    @DisplayName("Should generate different transaction hashes for different settlements")
    void shouldGenerateDifferentHashesForDifferentSettlements() {
        // Arrange
        Settlement settlement2 = new Settlement(202L, "buyer-002", "seller-002", 3.0, 4.0);
        settlement2.setId(2L);
        settlement2.setTotalAmount(12.0);

        // Act
        String hash1 = settlementService.generateTransactionHash(pendingSettlement);
        // Small delay to ensure different timestamp
        try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String hash2 = settlementService.generateTransactionHash(settlement2);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Should throw exception when completing non-pending settlement")
    void shouldThrowWhenCompletingNonPendingSettlement() {
        // Arrange
        pendingSettlement.setStatus(SettlementStatus.COMPLETED);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(pendingSettlement));

        // Act & Assert
        assertThatThrownBy(() -> settlementService.completeSettlement(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only complete PENDING settlements");
    }

    // ─── FAIL SETTLEMENT TESTS ────────────────────────────────────────────────

    @Test
    @DisplayName("Should fail a pending settlement successfully")
    void shouldFailPendingSettlement() {
        // Arrange
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(pendingSettlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Settlement failed = settlementService.failSettlement(1L, "Payment gateway error");

        // Assert
        assertThat(failed.getStatus()).isEqualTo(SettlementStatus.FAILED);
    }

    @Test
    @DisplayName("Should throw exception when failing a completed settlement")
    void shouldThrowWhenFailingCompletedSettlement() {
        // Arrange
        pendingSettlement.setStatus(SettlementStatus.COMPLETED);
        when(settlementRepository.findById(1L)).thenReturn(Optional.of(pendingSettlement));

        // Act & Assert
        assertThatThrownBy(() -> settlementService.failSettlement(1L, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only fail PENDING settlements");
    }

    @Test
    @DisplayName("Should throw exception when settlement not found")
    void shouldThrowWhenSettlementNotFound() {
        // Arrange
        when(settlementRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> settlementService.getSettlementById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Settlement not found");
    }

    // ─── AMOUNT VALIDATION TESTS ──────────────────────────────────────────────

    @Test
    @DisplayName("Should reject calculateSettlementAmount with negative energy")
    void shouldRejectNegativeEnergyInCalculation() {
        assertThatThrownBy(() -> settlementService.calculateSettlementAmount(-1.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Energy kWh must be positive");
    }

    @Test
    @DisplayName("Should correctly retrieve settlements by status")
    void shouldRetrieveSettlementsByStatus() {
        // Arrange
        when(settlementRepository.findByStatus(SettlementStatus.PENDING))
                .thenReturn(List.of(pendingSettlement));

        // Act
        List<Settlement> result = settlementService.getSettlementsByStatus(SettlementStatus.PENDING);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(SettlementStatus.PENDING);
    }
}
