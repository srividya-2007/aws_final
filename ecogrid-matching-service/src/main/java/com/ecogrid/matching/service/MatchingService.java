package com.ecogrid.matching.service;

import com.ecogrid.matching.model.*;
import com.ecogrid.matching.repository.EnergyListingRepository;
import com.ecogrid.matching.repository.EnergyOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MatchingService {

    @Autowired
    private EnergyListingRepository listingRepository;

    @Autowired
    private EnergyOrderRepository orderRepository;

    public EnergyListing createListing(EnergyListing listing) {
        if (listing.getAvailableEnergyKwh() <= 0) {
            throw new IllegalArgumentException("Available energy must be greater than 0");
        }
        if (listing.getPricePerKwh() <= 0) {
            throw new IllegalArgumentException("Price per kWh must be greater than 0");
        }
        listing.setStatus(ListingStatus.AVAILABLE);
        return listingRepository.save(listing);
    }

    public EnergyOrder placeOrder(EnergyOrder order) {
        if (order.getRequestedEnergyKwh() <= 0) {
            throw new IllegalArgumentException("Requested energy must be greater than 0");
        }
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Transactional
    public EnergyOrder matchOrder(Long orderId) {
        EnergyOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.MATCHED) {
            throw new IllegalStateException("Order is already matched");
        }

        // Find available listings that meet the price requirement
        List<EnergyListing> availableListings = listingRepository
                .findByStatusAndPricePerKwhLessThanEqual(ListingStatus.AVAILABLE, order.getMaxPricePerKwh());

        if (availableListings.isEmpty()) {
            order.setStatus(OrderStatus.FAILED);
            return orderRepository.save(order);
        }

        // Prevent duplicate matching
        for (EnergyListing listing : availableListings) {
            if (orderRepository.existsByBuyerIdAndMatchedListingId(order.getBuyerId(), listing.getId())) {
                continue; // Skip already matched listings for this buyer
            }

            double requested = order.getRequestedEnergyKwh();
            double available = listing.getAvailableEnergyKwh();

            if (available >= requested) {
                // Full match
                listing.setAvailableEnergyKwh(available - requested);
                listing.setStatus(available - requested == 0 ? ListingStatus.FULLY_MATCHED : ListingStatus.PARTIALLY_MATCHED);
                listingRepository.save(listing);

                order.setMatchedListingId(listing.getId());
                order.setMatchedEnergyKwh(requested);
                order.setStatus(OrderStatus.MATCHED);
                return orderRepository.save(order);
            } else if (available > 0) {
                // Partial match
                listing.setAvailableEnergyKwh(0.0);
                listing.setStatus(ListingStatus.FULLY_MATCHED);
                listingRepository.save(listing);

                order.setMatchedListingId(listing.getId());
                order.setMatchedEnergyKwh(available);
                order.setRequestedEnergyKwh(requested - available);
                order.setStatus(OrderStatus.PARTIALLY_MATCHED);
                return orderRepository.save(order);
            }
        }

        order.setStatus(OrderStatus.FAILED);
        return orderRepository.save(order);
    }

    public List<EnergyListing> getAvailableListings() {
        return listingRepository.findByStatus(ListingStatus.AVAILABLE);
    }

    public EnergyListing getListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + id));
    }

    public List<EnergyOrder> getOrdersByBuyer(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }
}
