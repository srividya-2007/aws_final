package com.ecogrid.matching.service;

import com.ecogrid.matching.model.*;
import com.ecogrid.matching.repository.EnergyListingRepository;
import com.ecogrid.matching.repository.EnergyOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Matching Service Unit Tests")
class MatchingServiceTest {

    @Mock
    private EnergyListingRepository listingRepository;

    @Mock
    private EnergyOrderRepository orderRepository;

    @InjectMocks
    private MatchingService matchingService;

    private EnergyListing availableListing;
    private EnergyOrder pendingOrder;

    @BeforeEach
    void setUp() {
        availableListing = new EnergyListing("seller-001", 10.0, 5.0);
        availableListing.setId(1L);
        availableListing.setStatus(ListingStatus.AVAILABLE);

        pendingOrder = new EnergyOrder("buyer-001", 6.0, 6.0);
        pendingOrder.setId(1L);
        pendingOrder.setStatus(OrderStatus.PENDING);
    }

    // ─── CREATE LISTING TESTS ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should create energy listing successfully")
    void shouldCreateEnergyListingSuccessfully() {
        // Arrange
        when(listingRepository.save(any(EnergyListing.class))).thenReturn(availableListing);

        // Act
        EnergyListing created = matchingService.createListing(availableListing);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getSellerId()).isEqualTo("seller-001");
        assertThat(created.getAvailableEnergyKwh()).isEqualTo(10.0);
        assertThat(created.getStatus()).isEqualTo(ListingStatus.AVAILABLE);
        verify(listingRepository).save(any(EnergyListing.class));
    }

    @Test
    @DisplayName("Should reject listing with zero energy available")
    void shouldRejectListingWithZeroEnergy() {
        // Arrange
        EnergyListing invalidListing = new EnergyListing("seller-002", 0.0, 5.0);

        // Act & Assert
        assertThatThrownBy(() -> matchingService.createListing(invalidListing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available energy must be greater than 0");
    }

    @Test
    @DisplayName("Should reject listing with negative price")
    void shouldRejectListingWithNegativePrice() {
        // Arrange
        EnergyListing invalidListing = new EnergyListing("seller-003", 5.0, -1.0);

        // Act & Assert
        assertThatThrownBy(() -> matchingService.createListing(invalidListing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price per kWh must be greater than 0");
    }

    // ─── PLACE ORDER TESTS ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should place consumer order successfully")
    void shouldPlaceConsumerOrderSuccessfully() {
        // Arrange
        when(orderRepository.save(any(EnergyOrder.class))).thenReturn(pendingOrder);

        // Act
        EnergyOrder placed = matchingService.placeOrder(pendingOrder);

        // Assert
        assertThat(placed).isNotNull();
        assertThat(placed.getBuyerId()).isEqualTo("buyer-001");
        assertThat(placed.getRequestedEnergyKwh()).isEqualTo(6.0);
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should reject order with zero requested energy")
    void shouldRejectOrderWithZeroEnergy() {
        // Arrange
        EnergyOrder invalidOrder = new EnergyOrder("buyer-002", 0.0, 5.0);

        // Act & Assert
        assertThatThrownBy(() -> matchingService.placeOrder(invalidOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requested energy must be greater than 0");
    }

    // ─── MATCHING TESTS ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should fully match an order when sufficient energy is available")
    void shouldFullyMatchOrderWhenEnergyIsSufficient() {
        // Arrange - listing has 10kWh, order needs 6kWh
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(listingRepository.findByStatusAndPricePerKwhLessThanEqual(
                eq(ListingStatus.AVAILABLE), anyDouble()))
                .thenReturn(List.of(availableListing));
        when(orderRepository.existsByBuyerIdAndMatchedListingId(anyString(), anyLong())).thenReturn(false);
        when(listingRepository.save(any())).thenReturn(availableListing);
        when(orderRepository.save(any(EnergyOrder.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        EnergyOrder result = matchingService.matchOrder(1L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.MATCHED);
        assertThat(result.getMatchedEnergyKwh()).isEqualTo(6.0);
        assertThat(result.getMatchedListingId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should partially match order when listing has less energy than requested")
    void shouldPartiallyMatchOrderWhenInsufficientEnergy() {
        // Arrange - listing has 3kWh, order needs 6kWh
        availableListing.setAvailableEnergyKwh(3.0);
        pendingOrder.setRequestedEnergyKwh(6.0);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(listingRepository.findByStatusAndPricePerKwhLessThanEqual(
                eq(ListingStatus.AVAILABLE), anyDouble()))
                .thenReturn(List.of(availableListing));
        when(orderRepository.existsByBuyerIdAndMatchedListingId(anyString(), anyLong())).thenReturn(false);
        when(listingRepository.save(any())).thenReturn(availableListing);
        when(orderRepository.save(any(EnergyOrder.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        EnergyOrder result = matchingService.matchOrder(1L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PARTIALLY_MATCHED);
        assertThat(result.getMatchedEnergyKwh()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should fail order when no available listings exist")
    void shouldFailOrderWhenNoListingsAvailable() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(listingRepository.findByStatusAndPricePerKwhLessThanEqual(
                eq(ListingStatus.AVAILABLE), anyDouble()))
                .thenReturn(Collections.emptyList());
        when(orderRepository.save(any(EnergyOrder.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        EnergyOrder result = matchingService.matchOrder(1L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("Should throw exception when matching non-existent order")
    void shouldThrowWhenMatchingNonExistentOrder() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> matchingService.matchOrder(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should throw exception when order is already matched")
    void shouldThrowWhenOrderAlreadyMatched() {
        // Arrange
        pendingOrder.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        // Act & Assert
        assertThatThrownBy(() -> matchingService.matchOrder(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order is already matched");
    }

    @Test
    @DisplayName("Should prevent duplicate matching for same buyer-listing combination")
    void shouldPreventDuplicateMatchingForSameBuyerListing() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(listingRepository.findByStatusAndPricePerKwhLessThanEqual(
                eq(ListingStatus.AVAILABLE), anyDouble()))
                .thenReturn(List.of(availableListing));
        // Simulate already matched
        when(orderRepository.existsByBuyerIdAndMatchedListingId("buyer-001", 1L)).thenReturn(true);
        when(orderRepository.save(any(EnergyOrder.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        EnergyOrder result = matchingService.matchOrder(1L);

        // Assert - since all listings are duplicate matches, order fails
        assertThat(result.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("Should retrieve all available listings")
    void shouldRetrieveAllAvailableListings() {
        // Arrange
        EnergyListing listing2 = new EnergyListing("seller-002", 5.0, 4.5);
        listing2.setId(2L);
        when(listingRepository.findByStatus(ListingStatus.AVAILABLE))
                .thenReturn(List.of(availableListing, listing2));

        // Act
        List<EnergyListing> listings = matchingService.getAvailableListings();

        // Assert
        assertThat(listings).hasSize(2);
        assertThat(listings).extracting(EnergyListing::getStatus)
                .containsOnly(ListingStatus.AVAILABLE);
    }
}
