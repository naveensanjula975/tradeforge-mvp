package com.tradeforge.instrument.service;

import com.tradeforge.common.exception.ConflictException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.common.exception.ResourceNotFoundException;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import com.tradeforge.instrument.domain.InstrumentStatus;
import com.tradeforge.instrument.web.dto.CreateInstrumentRequest;
import com.tradeforge.instrument.web.dto.InstrumentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> listAll() {
        return instrumentRepository.findAll().stream()
                .map(InstrumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getBySymbol(String symbol) {
        return instrumentRepository.findBySymbol(symbol.toUpperCase())
                .map(InstrumentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INSTRUMENT_NOT_FOUND,
                        "Instrument with symbol '" + symbol + "' not found."));
    }

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest request) {
        if (instrumentRepository.existsBySymbol(request.symbol().toUpperCase())) {
            throw new ConflictException(
                    ErrorCode.INSTRUMENT_SYMBOL_DUPLICATE,
                    "Instrument with symbol '" + request.symbol() + "' already exists.");
        }
        Instrument instrument = Instrument.create(
                request.symbol(), request.name(), request.tickSize(), request.lotSize());
        return InstrumentResponse.from(instrumentRepository.save(instrument));
    }

    @Transactional
    public InstrumentResponse updateStatus(String symbol, InstrumentStatus newStatus) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INSTRUMENT_NOT_FOUND,
                        "Instrument with symbol '" + symbol + "' not found."));

        if (newStatus == InstrumentStatus.ACTIVE) {
            instrument.activate();
        } else {
            instrument.deactivate();
        }
        return InstrumentResponse.from(instrumentRepository.save(instrument));
    }

    /**
     * Internal lookup used by order validation — returns the domain entity.
     */
    @Transactional(readOnly = true)
    public Instrument requireBySymbol(String symbol) {
        return instrumentRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INSTRUMENT_NOT_FOUND,
                        "Instrument with symbol '" + symbol + "' not found."));
    }
}
