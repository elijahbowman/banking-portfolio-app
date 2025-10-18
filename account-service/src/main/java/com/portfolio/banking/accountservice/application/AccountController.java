package com.portfolio.banking.accountservice.application;

import com.portfolio.banking.accountservice.domain.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestParam String fromAccountId,
            @RequestParam String toAccountId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.processTransfer(fromAccountId, toAccountId, amount));
    }
}