package com.tradeforge.instrument.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {

    Optional<Instrument> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    List<Instrument> findAllByStatus(InstrumentStatus status);
}
