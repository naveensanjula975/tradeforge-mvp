package com.tradeforge.account.service;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.account.web.dto.AccountResponse;
import com.tradeforge.account.web.dto.AccountStatementResponse;
import com.tradeforge.account.web.dto.PositionResponse;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.common.exception.ResourceNotFoundException;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final InstrumentRepository instrumentRepository;

    public AccountService(
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            InstrumentRepository instrumentRepository) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserId(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found for user ID: " + userId));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> getPositionsByUserId(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found for user ID: " + userId));

        List<Position> positions = positionRepository.findAllByAccountId(account.getId());
        return positions.stream()
                .map(pos -> {
                    Instrument instrument = instrumentRepository.findById(pos.getInstrumentId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    ErrorCode.INSTRUMENT_NOT_FOUND,
                                    "Instrument not found for ID: " + pos.getInstrumentId()));
                    BigDecimal marketValue = pos.getQuantity().multiply(pos.getAveragePrice());
                    return new PositionResponse(
                            pos.getId(),
                            pos.getAccountId(),
                            pos.getInstrumentId(),
                            instrument.getSymbol(),
                            pos.getQuantity(),
                            pos.getReservedQuantity(),
                            pos.getAveragePrice(),
                            marketValue
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found for user ID: " + userId));

        List<Position> positions = positionRepository.findAllByAccountId(account.getId());
        BigDecimal totalPositionValue = positions.stream()
                .map(pos -> pos.getQuantity().multiply(pos.getAveragePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPortfolioValue = account.getCashBalance().add(totalPositionValue);

        return new AccountStatementResponse(
                account.getId(),
                account.getCashBalance(),
                account.availableCash(),
                totalPositionValue,
                totalPortfolioValue,
                BigDecimal.ZERO // P&L defaults to 0 in MVP
        );
    }
}
