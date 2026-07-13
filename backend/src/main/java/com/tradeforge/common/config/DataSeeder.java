package com.tradeforge.common.config;

import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.account.domain.Position;
import com.tradeforge.account.domain.PositionRepository;
import com.tradeforge.auth.domain.User;
import com.tradeforge.auth.domain.UserRepository;
import com.tradeforge.auth.domain.UserRole;
import com.tradeforge.instrument.domain.Instrument;
import com.tradeforge.instrument.domain.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds demo data on startup when {@code app.seed.enabled=true} (local profile only).
 *
 * <h3>Demo credentials (never commit real passwords)</h3>
 * <ul>
 *   <li>Admin:   admin@tradeforge.local / Admin@1234</li>
 *   <li>Trader1: alice@tradeforge.local / Trader@1234</li>
 *   <li>Trader2: bob@tradeforge.local   / Trader@1234</li>
 * </ul>
 *
 * <h3>Demo instruments</h3>
 * <ul>
 *   <li>CAL — Caltex Lanka PLC</li>
 *   <li>JKH — John Keells Holdings</li>
 *   <li>COMB — Commercial Bank</li>
 * </ul>
 *
 * @see application-local.yml — {@code app.seed.enabled: true}
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository       userRepository;
    private final AccountRepository    accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final PositionRepository   positionRepository;
    private final PasswordEncoder      passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            InstrumentRepository instrumentRepository,
            PositionRepository positionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository       = userRepository;
        this.accountRepository    = accountRepository;
        this.instrumentRepository = instrumentRepository;
        this.positionRepository   = positionRepository;
        this.passwordEncoder      = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== TradeForge Data Seeder starting ===");

        seedInstruments();
        seedUsers();

        log.info("=== Data Seeder complete ===");
    }

    private void seedInstruments() {
        if (instrumentRepository.count() > 0) {
            log.info("Instruments already seeded — skipping.");
            return;
        }

        Instrument cal  = Instrument.create("CAL",  "Caltex Lanka PLC",       new BigDecimal("0.10"), new BigDecimal("10"));
        Instrument jkh  = Instrument.create("JKH",  "John Keells Holdings",   new BigDecimal("0.10"), new BigDecimal("10"));
        Instrument comb = Instrument.create("COMB", "Commercial Bank of Ceylon", new BigDecimal("0.10"), new BigDecimal("10"));

        instrumentRepository.save(cal);
        instrumentRepository.save(jkh);
        instrumentRepository.save(comb);

        log.info("Seeded instruments: CAL, JKH, COMB");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already seeded — skipping.");
            return;
        }

        // Admin
        User admin = User.create("Admin", "admin@tradeforge.local",
                passwordEncoder.encode("Admin@1234"), UserRole.ADMIN);
        userRepository.save(admin);
        Account adminAccount = Account.create(admin.getId(), new BigDecimal("100000.0000"));
        accountRepository.save(adminAccount);

        // Trader Alice (cash-rich, no positions — buyer scenario)
        User alice = User.create("Alice Trader", "alice@tradeforge.local",
                passwordEncoder.encode("Trader@1234"), UserRole.TRADER);
        userRepository.save(alice);
        Account aliceAccount = Account.create(alice.getId(), new BigDecimal("500000.0000"));
        accountRepository.save(aliceAccount);

        // Trader Bob (positions pre-loaded — seller scenario)
        User bob = User.create("Bob Trader", "bob@tradeforge.local",
                passwordEncoder.encode("Trader@1234"), UserRole.TRADER);
        userRepository.save(bob);
        Account bobAccount = Account.create(bob.getId(), new BigDecimal("50000.0000"));
        accountRepository.save(bobAccount);

        // Give Bob initial positions in all three instruments
        for (Instrument instrument : instrumentRepository.findAll()) {
            Position position = Position.create(
                    bobAccount.getId(),
                    instrument.getId(),
                    new BigDecimal("1000"),
                    new BigDecimal("100.00"));
            positionRepository.save(position);
        }

        log.info("Seeded users: admin, alice, bob (bob has positions in CAL/JKH/COMB)");
        log.warn("Demo credentials active — NEVER use this profile in production!");
    }
}
