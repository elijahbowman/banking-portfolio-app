package com.portfolio.banking.bankingservice.application;

import com.portfolio.banking.bankingservice.domain.service.BankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            // Call service with validated parameters
            Map<String, Object> result = bankingService.processDeposit(accountId.trim(), amount);
            log.info("Deposit successful - Transaction ID: {}", result.get("transactionId"));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Service validation failed - Account: {}, Error: {}", accountId, e.getMessage());
            return createValidationError(e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error for account {}: {}", accountId, e.getMessage(), e);
            return createServerError();
        }
    }

    private ResponseEntity<Map<String, Object>> createValidationError(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", "VALIDATION_ERROR");
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    private ResponseEntity<Map<String, Object>> createServerError() {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("status", "INTERNAL_ERROR");
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = bankingService.getSystemHealth();
        health.put("service", "BankingService");
        health.put("status", "HEALTHY");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "BankingService");
        info.put("version", "1.0.0");
        info.put("description", "Banking microservice for portfolio demonstration");
        info.put("kafkaEnabled", true);
        info.put("database", "H2");
        info.put("endpoints", new String[]{
                "POST /api/v1/banking/deposits",
                "GET /api/v1/banking/health",
                "GET /api/v1/banking/info"
        });
        info.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(info);
    }
}