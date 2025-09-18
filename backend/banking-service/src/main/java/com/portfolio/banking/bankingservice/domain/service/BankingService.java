package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BankingService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void initiateDeposit(String accountId, BigDecimal amount) {
        log.info("Processing deposit for account: {}, amount: {}", accountId, amount);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid amount: {}", amount); // Prompt ID 061
            throw new IllegalArgumentException("Amount must be positive");
        }
        Account account = accountRepository.findById(accountId)
                .orElse(new Account(accountId, BigDecimal.ZERO));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Account updated: {}, new balance: {}", accountId, account.getBalance());

        kafkaTemplate.send("deposit-events", accountId, "Deposit: " + amount);
        log.debug("Published deposit event for account: {}", accountId);
    }
}