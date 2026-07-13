package com.tradeforge.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    Optional<Position> findByAccountIdAndInstrumentId(UUID accountId, UUID instrumentId);

    List<Position> findAllByAccountId(UUID accountId);
}
