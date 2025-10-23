package com.portfolio.banking.bankingservice.application;

import com.portfolio.banking.bankingservice.domain.service.BankingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
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

    @Data
    public static class TransferRequest {
        @NotNull(message = "From account ID must not be null")
        private String fromAccountId;
        @NotNull(message = "To account ID must not be null")
        private String toAccountId;
        @NotNull(message = "Amount must not be null")
        private BigDecimal amount;
    }

    @Data
    public static class DepositRequest {
        @NotNull(message = "Account ID must not be null")
        private String accountId;
        @NotNull(message = "Amount must not be null")
        private BigDecimal amount;
    }

    @Data
    public static class WithdrawalRequest {
        @NotNull(message = "Account ID must not be null")
        private String accountId;
        @NotNull(message = "Amount must not be null")
        private BigDecimal amount;
    }

    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> initiateTransfer(@Valid @RequestBody TransferRequest request) {
        Map<String, Object> response = bankingService.initiateTransfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposits")
    public ResponseEntity<Map<String, Object>> initiateDeposit(@Valid @RequestBody DepositRequest request) {
        Map<String, Object> response = bankingService.initiateDeposit(
                request.getAccountId(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<Map<String, Object>> initiateWithdrawal(@Valid @RequestBody WithdrawalRequest request) {
        Map<String, Object> response = bankingService.initiateWithdrawal(
                request.getAccountId(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountId) {
        BigDecimal balance = bankingService.getBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance
        ));
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