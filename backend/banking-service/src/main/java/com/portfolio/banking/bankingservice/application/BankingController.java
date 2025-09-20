package com.portfolio.banking.bankingservice.application;

import com.portfolio.banking.bankingservice.domain.service.BankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/banking")
@RequiredArgsConstructor
@Slf4j
public class BankingController {

    private final BankingService bankingService;

    @PostMapping("/deposits")
    public ResponseEntity<Map<String, Object>> deposit(
            @RequestParam String accountId,
            @RequestParam BigDecimal amount) {

        log.info("Received deposit request - Account: {}, Amount: {}", accountId, amount);

        try {
            Map<String, Object> result = bankingService.processDeposit(accountId, amount);
            log.info("Deposit processed successfully - Transaction ID: {}", result.get("transactionId"));

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid deposit request - Account: {}, Error: {}", accountId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "VALIDATION_ERROR");
            error.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("Unexpected error processing deposit for account {}: {}", accountId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("status", "INTERNAL_ERROR");
            error.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = bankingService.getSystemHealth();
        health.put("service", "BankingService");
        health.put("status", "HEALTHY");
        health.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "BankingService");
        info.put("version", "1.0.0");
        info.put("description", "Banking microservice for portfolio demonstration");
        info.put("endpoints", new String[]{"/api/v1/banking/deposits", "/api/v1/banking/health"});
        return ResponseEntity.ok(info);
    }
}