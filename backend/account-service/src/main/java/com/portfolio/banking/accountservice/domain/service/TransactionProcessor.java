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

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public Map<String, Object> processTransfer(String transactionId, String fromAccountId, String toAccountId, BigDecimal amount) {
        log.info("Processing transfer: transactionId={}, fromAccountId={}, toAccountId={}, amount={}",
                transactionId, fromAccountId, toAccountId, amount);

        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            eventPublisher.publishRollbackEvent(transactionId, "TRANSFER", fromAccountId, toAccountId, null, amount);
            throw new IllegalStateException("Insufficient balance for transfer: " + transactionId);
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return completeTransaction(transactionId, "TRANSFER");
    }

    @Transactional
    public Map<String, Object> processDeposit(String transactionId, String accountId, BigDecimal amount) {
        log.info("Processing deposit: transactionId={}, accountId={}, amount={}", transactionId, accountId, amount);

        Account account = getAccount(accountId);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        return completeTransaction(transactionId, "DEPOSIT");
    }

    @Transactional
    public Map<String, Object> processWithdrawal(String transactionId, String accountId, BigDecimal amount) {
        log.info("Processing withdrawal: transactionId={}, accountId={}, amount={}", transactionId, accountId, amount);

        Account account = getAccount(accountId);
        if (account.getBalance().compareTo(amount) < 0) {
            eventPublisher.publishRollbackEvent(transactionId, "WITHDRAWAL", null, null, accountId, amount);
            throw new IllegalStateException("Insufficient balance for withdrawal: " + transactionId);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        return completeTransaction(transactionId, "WITHDRAWAL");
    }

    private Map<String, Object> completeTransaction(String transactionId, String transactionType) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        eventPublisher.publishCompletedEvent(
                transactionId,
                transactionType,
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAccountId(),
                transaction.getAmount()
        );

        return Map.of(
                "transactionId", transactionId,
                "status", "COMPLETED",
                "transactionType", transactionType
        );
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }
}