package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transfer;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Map<String, Object> processTransfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        validateTransfer(fromAccountId, toAccountId, amount);

        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transfer transfer = createTransferRecord(fromAccountId, toAccountId, amount);
        publishTransferEvent(transfer);

        return buildTransferResponse(transfer);
    }

    @Transactional
    public Transfer processTransfer(String transferId, String fromAccountId, String toAccountId, BigDecimal amount) {

        validateTransfer(fromAccountId, toAccountId, amount);

        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            publishRollbackEvent(transferId, fromAccountId, toAccountId, amount);
            throw new IllegalStateException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transfer transfer = transferRepository.findById(transferId).orElseThrow();
        transfer.setStatus("COMPLETED");
        transferRepository.save(transfer);

        publishTransferEvent(transfer);

        return transfer;
    }

    @KafkaListener(topics = "transfer-events", groupId = "account-group")
    @Transactional
    public void handleTransferEvent(Map<String, Object> event) {
        log.info("Handling transfer event: {}", event);

        String eventType = (String) event.get("eventType");
        String transferId = (String) event.get("transferId");
        String fromAccountId = (String) event.get("fromAccountId");
        String toAccountId = (String) event.get("toAccountId");
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        String status = (String) event.get("status");

        if ("TRANSFER_INITIATED".equals(eventType)) {
            try {
                // Create and save Transfer entity
                Transfer transfer = new Transfer();
                transfer.setTransferId(transferId);
                transfer.setFromAccountId(fromAccountId);
                transfer.setToAccountId(toAccountId);
                transfer.setAmount(amount);
                transfer.setStatus("PENDING");
                try {
                    transferRepository.save(transfer);
                } catch (DataIntegrityViolationException e) {
                    log.warn("Transfer {} already exists, proceeding with processing", transferId);
                    // Continue if duplicate (idempotency)
                }

                // Process the transfer
                processTransfer(transferId, fromAccountId, toAccountId, amount);
            } catch (Exception e) {
                log.error("Failed to process transfer {}: {}", transferId, e.getMessage());
                publishRollbackEvent(transferId, fromAccountId, toAccountId, amount);
            }
        } else {
            log.warn("Unknown event type: {}", eventType);
        }
    }

    private void validateTransfer(String from, String to, BigDecimal amount) {
        if (from == null || to == null || from.equals(to)) {
            throw new IllegalArgumentException("Invalid accounts");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    System.out.println("Database state after account not found: Accounts=" + accountRepository.findAll() + " Transfers=" + transferRepository.findAll());
                    return new IllegalArgumentException("Account not found: " + accountId);
                });
    }

    private Transfer createTransferRecord(String from, String to, BigDecimal amount) {
        Transfer transfer = new Transfer();
        transfer.setTransferId(UUID.randomUUID().toString());
        transfer.setFromAccountId(from);
        transfer.setToAccountId(to);
        transfer.setAmount(amount);
        transfer.setStatus("COMPLETED");
        return transferRepository.save(transfer);
    }

    private Transfer createTransferRecord(String transferId, String from, String to, BigDecimal amount) {
        Transfer transfer = new Transfer();
        transfer.setTransferId(transferId);
        transfer.setFromAccountId(from);
        transfer.setToAccountId(to);
        transfer.setAmount(amount);
        transfer.setStatus("COMPLETED");
        return transferRepository.save(transfer);
    }

    private void publishTransferEvent(Transfer transfer) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_STATUS");
        event.put("transferId", transfer.getTransferId());
        event.put("fromAccountId", transfer.getFromAccountId());
        event.put("toAccountId", transfer.getToAccountId());
        event.put("amount", transfer.getAmount().toString());
        event.put("status", transfer.getStatus());
        event.put("timestamp", LocalDateTime.now().toString());
        kafkaTemplate.send("transfer-events", transfer.getTransferId(), event);
        log.info("Published transfer event: {}", event);
    }

    private void publishRollbackEvent(String transferId, String fromAccountId, String toAccountId, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transferId", transferId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("amount", amount);
        kafkaTemplate.send("rollback-events", transferId, event);
        log.info("Published rollback event: {}", event);
    }

    private Map<String, Object> buildTransferResponse(Transfer transfer) {
        Map<String, Object> response = new HashMap<>();
        response.put("transferId", transfer.getTransferId());
        response.put("fromAccountId", transfer.getFromAccountId());
        response.put("toAccountId", transfer.getToAccountId());
        response.put("amount", transfer.getAmount());
        response.put("status", transfer.getStatus());
        return response;
    }

    @KafkaListener(topics = "rollback-events", groupId = "account-group")
    @Transactional
    public void handleRollback(Map<String, Object> event) {
        log.info("Handling rollback event: {}", event);

        String transferId = (String) event.get("transferId");
        String fromAccountId = (String) event.get("fromAccountId");
        String toAccountId = (String) event.get("toAccountId");
        BigDecimal amount = new BigDecimal(event.get("amount").toString());

        Optional<Transfer> transferOpt = transferRepository.findById(transferId);
        if (transferOpt.isPresent()) {
            Transfer transfer = transferOpt.get();
            if ("COMPLETED".equals(transfer.getStatus())) {
                Account fromAccount = getAccount(fromAccountId);
                Account toAccount = getAccount(toAccountId);

                fromAccount.setBalance(fromAccount.getBalance().add(amount));
                toAccount.setBalance(toAccount.getBalance().subtract(amount));
                transfer.setStatus("ROLLED_BACK");

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);
                transferRepository.save(transfer);
                log.info("Rolled back transfer {}: Restored {} to fromAccount, deducted from toAccount", transferId, amount);
            } else {
                log.warn("Transfer {} is not in COMPLETED state, ignoring rollback", transferId);
                System.out.println("Database state after ignoring rollback: Accounts=" + accountRepository.findAll() + " Transfers=" + transferRepository.findAll());
            }
        } else {
            log.error("Transfer {} not found for rollback", transferId);
        }
    }

    private BigDecimal getAmountFromEvent(Map<String, Object> event) {
        Object amountObj = event.get("amount");
        if (amountObj instanceof BigDecimal) {
            return (BigDecimal) amountObj;
        } else if (amountObj instanceof String) {
            try {
                return new BigDecimal((String) amountObj);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid amount format in event: " + amountObj, e);
            }
        } else if (amountObj instanceof Double) {
            return BigDecimal.valueOf((Double) amountObj);
        } else {
            throw new IllegalArgumentException("Amount must be a String, BigDecimal, or Double, got: " + amountObj);
        }
    }
}