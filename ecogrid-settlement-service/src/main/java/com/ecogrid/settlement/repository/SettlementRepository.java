package com.ecogrid.settlement.repository;

import com.ecogrid.settlement.model.Settlement;
import com.ecogrid.settlement.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
    List<Settlement> findByStatus(SettlementStatus status);
    List<Settlement> findByBuyerId(String buyerId);
    Optional<Settlement> findByTransactionHash(String transactionHash);
}
