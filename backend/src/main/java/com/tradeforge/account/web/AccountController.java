package com.tradeforge.account.web;

import com.tradeforge.account.service.AccountService;
import com.tradeforge.account.web.dto.AccountResponse;
import com.tradeforge.account.web.dto.AccountStatementResponse;
import com.tradeforge.account.web.dto.PositionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public AccountResponse getAccount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return accountService.getAccountByUserId(userId);
    }

    @GetMapping("/positions")
    public List<PositionResponse> getPositions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return accountService.getPositionsByUserId(userId);
    }

    @GetMapping("/statement")
    public AccountStatementResponse getStatement(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return accountService.getAccountStatement(userId);
    }
}
