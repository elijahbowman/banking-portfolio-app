package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transaction;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRollbackHandler {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void handleRollback(Map<String, Object> event) {
        log.info("Handling rollback event: {}", event);

        String transactionId = (String) event.get("transactionId");
        String eventType = (String) event.get("eventType");

        if (!"ROLLBACK".equals(eventType)) {
            log.warn("Unknown event type: {}", eventType);
            return;
        }

        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            log.error("Transaction {} not found for rollback", transactionId);
            return;
        }

        Transaction transaction = transactionOpt.get();
        if (!"COMPLETED".equals(transaction.getStatus())) {
            log.warn("Transaction {} is not in COMPLETED state, ignoring rollback", transactionId);
            return;
        }

        String transactionType = transaction.getTransactionType();
        BigDecimal amount = transaction.getAmount();

        if ("TRANSFER".equals(transactionType)) {
            String fromAccountId = transaction.getFromAccountId();
            String toAccountId = transaction.getToAccountId();
            Account fromAccount = getAccount(fromAccountId);
            Account toAccount = getAccount(toAccountId);

            fromAccount.setBalance(fromAccount.getBalance().add(amount));
            toAccount.setBalance(toAccount.getBalance().subtract(amount));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
        } else if ("DEPOSIT".equals(transactionType)) {
            String accountId = transaction.getAccountId();
            Account account = getAccount(accountId);
            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);
        } else if ("WITHDRAWAL".equals(transactionType)) {
            String accountId = transaction.getAccountId();
            Account account = getAccount(accountId);
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);
        }

        transaction.setStatus("ROLLED_BACK");
        transactionRepository.save(transaction);
        log.info("Rolled back transaction {}: Adjusted balance", transactionId);
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }
}