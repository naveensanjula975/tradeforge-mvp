package com.tradeforge.account.service;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.account.web.dto.AccountResponse;
import com.tradeforge.account.web.dto.AccountStatementResponse;
import com.tradeforge.account.web.dto.PositionResponse;
import com.tradeforge.common.exception.ResourceNotFoundException;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account service tests")
class AccountServiceTest {

    private AccountRepository accountRepository;
    private PositionRepository positionRepository;
    private InstrumentRepository instrumentRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        positionRepository = Mockito.mock(PositionRepository.class);
        instrumentRepository = Mockito.mock(InstrumentRepository.class);
        accountService = new AccountService(accountRepository, positionRepository, instrumentRepository);
    }

    @Test
    @DisplayName("getAccountByUserId returns AccountResponse when account exists")
    void getAccountByUserId_exists_returnsResponse() {
        UUID userId = UUID.randomUUID();
        Account account = Account.create(userId, new BigDecimal("1000.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByUserId(userId);

        assertThat(response).isNotNull();
        assertThat(response.cashBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("getAccountByUserId throws ResourceNotFoundException when account does not exist")
    void getAccountByUserId_notExists_throwsException() {
        UUID userId = UUID.randomUUID();
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPositionsByUserId returns positions with resolved symbols")
    void getPositionsByUserId_exists_returnsResolvedPositions() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();

        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(accountId);
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        Position position = Position.create(accountId, instrumentId, new BigDecimal("50"), new BigDecimal("100.00"));
        Mockito.when(positionRepository.findAllByAccountId(accountId)).thenReturn(List.of(position));

        Instrument instrument = Instrument.create("CAL", "Caltex Lanka PLC", new BigDecimal("0.10"), new BigDecimal("10"));
        Mockito.when(instrumentRepository.findById(instrumentId)).thenReturn(Optional.of(instrument));

        List<PositionResponse> responses = accountService.getPositionsByUserId(userId);

        assertThat(responses).hasSize(1);
        PositionResponse response = responses.get(0);
        assertThat(response.symbol()).isEqualTo("CAL");
        assertThat(response.quantity()).isEqualByComparingTo("50");
        assertThat(response.marketValue()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("getAccountStatement calculates position value and portfolio value correctly")
    void getAccountStatement_exists_calculatesCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();

        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(accountId);
        Mockito.when(account.getCashBalance()).thenReturn(new BigDecimal("1000.00"));
        Mockito.when(account.availableCash()).thenReturn(new BigDecimal("900.00"));
        Mockito.when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        Position position = Position.create(accountId, instrumentId, new BigDecimal("10"), new BigDecimal("50.00"));
        Mockito.when(positionRepository.findAllByAccountId(accountId)).thenReturn(List.of(position));

        AccountStatementResponse response = accountService.getAccountStatement(userId);

        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
        assertThat(response.buyingPower()).isEqualByComparingTo("900.00");
        assertThat(response.totalPositionValue()).isEqualByComparingTo("500.00");
        assertThat(response.totalPortfolioValue()).isEqualByComparingTo("1500.00");
    }
}
