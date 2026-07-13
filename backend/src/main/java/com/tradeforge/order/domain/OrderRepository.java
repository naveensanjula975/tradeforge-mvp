package com.tradeforge.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByUserIdAndClientOrderId(UUID userId, String clientOrderId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Active orders for a given instrument — used to rebuild the order book on startup.
     * Sorted by sequence number for deterministic replay.
     */
    @Query("""
           SELECT o FROM Order o
           WHERE o.instrumentId = :instrumentId
             AND o.status IN ('ACCEPTED', 'PARTIALLY_FILLED')
           ORDER BY o.sequenceNumber ASC
           """)
    List<Order> findActiveByInstrumentIdOrderBySequenceNumber(@Param("instrumentId") UUID instrumentId);

    /**
     * All active orders across all instruments — used for full order book rebuild on startup.
     */
    @Query("""
           SELECT o FROM Order o
           WHERE o.status IN ('ACCEPTED', 'PARTIALLY_FILLED')
           ORDER BY o.instrumentId, o.sequenceNumber ASC
           """)
    List<Order> findAllActiveOrderByInstrumentAndSequence();
}
