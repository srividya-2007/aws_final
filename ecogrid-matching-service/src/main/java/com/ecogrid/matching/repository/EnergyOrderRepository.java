package com.ecogrid.matching.repository;

import com.ecogrid.matching.model.EnergyOrder;
import com.ecogrid.matching.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyOrderRepository extends JpaRepository<EnergyOrder, Long> {
    List<EnergyOrder> findByBuyerId(String buyerId);
    List<EnergyOrder> findByStatus(OrderStatus status);
    boolean existsByBuyerIdAndMatchedListingId(String buyerId, Long listingId);
}
