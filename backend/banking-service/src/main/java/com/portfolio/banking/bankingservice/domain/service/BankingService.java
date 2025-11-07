package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.domain.exception.BalanceFetchException;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${banking.account-service-url:http://account-service:8081}")
    private String accountServiceUrl;

    @Transactional
    public Map<String, Object> initiateTransfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        validateTransfer(fromAccountId, toAccountId, amount);

        accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromAccountId));
        accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toAccountId));

        String transactionId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transactionId", transactionId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("amount", amount);
        event.put("transactionType", "TRANSFER");
        event.put("status", "PENDING");

        try {
            kafkaTemplate.send("transaction-events", transactionId, event);
            log.info("Initiated transfer: {}", event);
            return Map.of("transactionId", transactionId, "status", "PENDING");
        } catch (Exception e) {
            log.error("Failed to initiate transfer: {}", e.getMessage());
            throw new IllegalStateException("Transfer initiation failed", e);
        }
    }

    @Transactional
    public Map<String, Object> initiateDeposit(String accountId, BigDecimal amount) {
        validateDeposit(accountId, amount);

        findOrCreateAccount(accountId);

        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        String transactionId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DEPOSIT_INITIATED");
        event.put("transactionId", transactionId);
        event.put("accountId", accountId);
        event.put("amount", amount);
        event.put("transactionType", "DEPOSIT");
        event.put("status", "PENDING");

        try {
            kafkaTemplate.send("transaction-events", transactionId, event);
            log.info("Initiated deposit: {}", event);
            return Map.of("transactionId", transactionId, "status", "PENDING");
        } catch (Exception e) {
            log.error("Failed to initiate deposit: {}", e.getMessage());
            throw new IllegalStateException("Deposit initiation failed", e);
        }
    }

    @Transactional
    public Map<String, Object> initiateWithdrawal(String accountId, BigDecimal amount) {
        validateDeposit(accountId, amount);

        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        String transactionId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WITHDRAWAL_INITIATED");
        event.put("transactionId", transactionId);
        event.put("accountId", accountId);
        event.put("amount", amount);
        event.put("transactionType", "WITHDRAWAL");
        event.put("status", "PENDING");

        try {
            kafkaTemplate.send("transaction-events", transactionId, event);
            log.info("Initiated withdrawal: {}", event);
            return Map.of("transactionId", transactionId, "status", "PENDING");
        } catch (Exception e) {
            log.error("Failed to initiate withdrawal: {}", e.getMessage());
            throw new IllegalStateException("Withdrawal initiation failed", e);
        }
    }

    public BigDecimal getBalance(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID must not be null");
        }

        String url = accountServiceUrl + "/api/v1/accounts/balance?accountId=" + accountId;

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BalanceFetchException("Response body is null for account: " + accountId);
            }
            if (!responseBody.containsKey("balance")) {
                throw new BalanceFetchException("Failed to fetch balance for account: " + accountId);
            }
            try {
                return new BigDecimal(responseBody.get("balance").toString());
            } catch (NumberFormatException e) {
                log.error("Invalid balance format for account {}: {}", accountId, responseBody.get("balance"));
                throw new BalanceFetchException("Invalid balance format for account: " + accountId, e);
            }
        } catch (RestClientException e) {
            log.error("Error fetching balance for account {}: {}", accountId, e.getMessage());
            throw new BalanceFetchException("Failed to fetch balance for account: " + accountId, e);
        }
    }

    private void validateTransfer(String from, String to, BigDecimal amount) {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            throw new IllegalArgumentException("Account IDs must not be null or empty");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid accounts");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void validateDeposit(String accountId, BigDecimal amount) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID must not be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
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
}