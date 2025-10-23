package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transaction;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionProcessor transactionProcessor;
    private final TransactionRollbackHandler rollbackHandler;

    public BigDecimal getBalance(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID must not be null");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        return account.getBalance();
    }

    @KafkaListener(topics = "transaction-events", groupId = "account-group")
    public void handleTransactionEvent(Map<String, Object> event) {
        log.info("Handling transaction event: {}", event);

        String eventType = (String) event.get("eventType");
        if (eventType == null) {
            log.error("Event type is missing in event: {}", event);
            return;
        }

        if (!"TRANSFER_INITIATED".equals(eventType) &&
                !"DEPOSIT_INITIATED".equals(eventType) &&
                !"WITHDRAWAL_INITIATED".equals(eventType)) {
            log.warn("Unknown event type: {}", eventType);
            return;
        }

        // Extract and validate required fields
        String transactionId = (String) event.get("transactionId");
        String transactionType = (String) event.get("transactionType");
        Object amountObj = event.get("amount");
        String fromAccountId = (String) event.get("fromAccountId");
        String toAccountId = (String) event.get("toAccountId");
        String accountId = (String) event.get("accountId");

        if (transactionId == null || transactionType == null || amountObj == null) {
            log.error("Missing required fields in event: transactionId={}, transactionType={}, amount={}",
                    transactionId, transactionType, amountObj);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Invalid amount format in event: {}", event);
            return;
        }

        // Validate transaction type
        if (!"TRANSFER".equals(transactionType) &&
                !"DEPOSIT".equals(transactionType) &&
                !"WITHDRAWAL".equals(transactionType)) {
            log.error("Invalid transaction type: {}", transactionType);
            return;
        }

        // Validate account IDs based on transaction type
        if ("TRANSFER".equals(transactionType) && (fromAccountId == null || toAccountId == null)) {
            log.error("Missing account IDs for transfer: fromAccountId={}, toAccountId={}",
                    fromAccountId, toAccountId);
            return;
        }
        if (("DEPOSIT".equals(transactionType) || "WITHDRAWAL".equals(transactionType)) && accountId == null) {
            log.error("Missing accountId for {}: accountId={}", transactionType, accountId);
            return;
        }

        try {
            // Create and save transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionId(transactionId);
            transaction.setFromAccountId(fromAccountId);
            transaction.setToAccountId(toAccountId);
            transaction.setAccountId(accountId);
            transaction.setAmount(amount);
            transaction.setTransactionType(transactionType);
            transaction.setStatus("PENDING");
            try {
                transactionRepository.save(transaction);
            } catch (DataIntegrityViolationException e) {
                log.warn("Transaction {} already exists, proceeding with processing", transactionId);
            }

            // Process transaction based on type
            if ("TRANSFER".equals(transactionType)) {
                transactionProcessor.processTransfer(transactionId, fromAccountId, toAccountId, amount);
            } else if ("DEPOSIT".equals(transactionType)) {
                transactionProcessor.processDeposit(transactionId, accountId, amount);
            } else {
                transactionProcessor.processWithdrawal(transactionId, accountId, amount);
            }
        } catch (Exception e) {
            log.error("Failed to process transaction {}: {}", transactionId, e.getMessage());
        }
    }

    @KafkaListener(topics = "rollback-events", groupId = "account-group")
    public void handleRollbackEvent(Map<String, Object> event) {
        rollbackHandler.handleRollback(event);
    }
}