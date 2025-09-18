package com.portfolio.banking.bankingservice.application;

import com.portfolio.banking.bankingservice.domain.service.BankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/banking")
@Slf4j
public class BankingController {
    @Autowired
    private BankingService bankingService;

    @PostMapping("/deposits")
    public ResponseEntity<String> initiateDeposit(@RequestParam String accountId, @RequestParam BigDecimal amount) {
        log.info("Initiating deposit for account: {}, amount: {}", accountId, amount);
        try {
            bankingService.initiateDeposit(accountId, amount);
            log.info("Deposit successful for account: {}", accountId);
            return ResponseEntity.ok("Deposit successful for account: " + accountId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid deposit request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}