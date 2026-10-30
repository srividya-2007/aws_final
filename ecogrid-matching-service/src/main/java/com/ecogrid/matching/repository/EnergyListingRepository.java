package com.ecogrid.matching.repository;

import com.ecogrid.matching.model.EnergyListing;
import com.ecogrid.matching.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyListingRepository extends JpaRepository<EnergyListing, Long> {
    List<EnergyListing> findByStatus(ListingStatus status);
    List<EnergyListing> findBySellerId(String sellerId);
    List<EnergyListing> findByStatusAndPricePerKwhLessThanEqual(ListingStatus status, Double maxPrice);
}
