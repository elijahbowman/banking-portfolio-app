package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankingService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Map<String, Object> processDeposit(String accountId, BigDecimal amount) {
        log.info("Processing deposit request - Account: {}, Amount: {}", accountId, amount);

        validateDepositRequest(accountId, amount);

        Account account = findOrCreateAccount(accountId);

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = account.deposit(amount);

        account.setUpdatedAt(LocalDateTime.now());
        Account savedAccount = accountRepository.save(account);

        publishDepositEvent(accountId, amount, oldBalance, newBalance);

        log.info("Deposit completed - Account: {}, Old Balance: {}, New Balance: {}, Transaction ID: {}",
                accountId, oldBalance, newBalance, UUID.randomUUID());

        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", UUID.randomUUID().toString());
        result.put("accountId", accountId);
        result.put("amount", amount);
        result.put("oldBalance", oldBalance);
        result.put("newBalance", newBalance);
        result.put("status", "COMPLETED");
        result.put("timestamp", LocalDateTime.now());

        return result;
    }

    private void validateDepositRequest(String accountId, BigDecimal amount) {
        if (accountId == null || accountId.trim().isEmpty()) {
            log.error("Invalid account ID: {}", accountId);
            throw new IllegalArgumentException("Account ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid deposit amount: {}", amount);
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (amount.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            log.warn("Large deposit detected: {}", amount);
            // For demo purposes - in production, trigger fraud detection
        }
    }

    private Account findOrCreateAccount(String accountId) {
        Optional<Account> existingAccount = accountRepository.findById(accountId);

        if (existingAccount.isPresent()) {
            log.debug("Found existing account: {}", accountId);
            return existingAccount.get();
        }

        log.info("Creating new account: {}", accountId);
        Account newAccount = new Account();
        newAccount.setAccountId(accountId);
        newAccount.setAccountNumber(generateAccountNumber(accountId));
        newAccount.setCustomerName("Portfolio Customer " + accountId);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setCreatedAt(LocalDateTime.now());

        return accountRepository.save(newAccount);
    }

    private void publishDepositEvent(String accountId, BigDecimal amount,
                                     BigDecimal oldBalance, BigDecimal newBalance) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DEPOSIT_COMPLETED");
        event.put("accountId", accountId);
        event.put("amount", amount);
        event.put("oldBalance", oldBalance);
        event.put("newBalance", newBalance);
        event.put("timestamp", LocalDateTime.now());
        event.put("transactionId", UUID.randomUUID().toString());

        try {
            kafkaTemplate.send("deposit-events", accountId, event);
            log.debug("Published deposit event for account: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish deposit event for account {}: {}", accountId, e.getMessage());
            // In production: trigger dead letter queue or retry mechanism
            throw new RuntimeException("Event publishing failed", e);
        }
    }

    private String generateAccountNumber(String accountId) {
        return "ACC" + accountId.substring(accountId.length() - 4).toUpperCase();
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("totalAccounts", accountRepository.count());
        health.put("totalBalance", accountRepository.getTotalSystemBalance());
        health.put("activeAccounts", accountRepository.countActiveAccounts());
        health.put("timestamp", LocalDateTime.now());
        return health;
    }

    @Transactional
    public Map<String, Object> initiateTransfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        validateTransfer(fromAccountId, toAccountId, amount);

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toAccountId));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        String transferId = UUID.randomUUID().toString();
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transferId", transferId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("amount", amount);
        event.put("status", "PENDING");

        try {
            kafkaTemplate.send("transfer-events", transferId, event);
            log.info("Initiated transfer: {}", event);
            return Map.of("transferId", transferId, "status", "PENDING");
        } catch (Exception e) {
            log.error("Failed to initiate transfer: {}", e.getMessage());
            publishRollbackEvent(transferId, fromAccountId, toAccountId, amount);
            throw new IllegalStateException("Transfer initiation failed", e);
        }
    }

    private void publishRollbackEvent(String transferId, String fromAccountId, String toAccountId, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transferId", transferId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("amount", amount.toString());
        event.put("timestamp", LocalDateTime.now().toString());
        kafkaTemplate.send("rollback-events", transferId, event);
        log.info("Published rollback event: {}", event);
    }

    private void validateTransfer(String from, String to, BigDecimal amount) {
        if (from == null || to == null || from.equals(to)) {
            throw new IllegalArgumentException("Invalid accounts");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}