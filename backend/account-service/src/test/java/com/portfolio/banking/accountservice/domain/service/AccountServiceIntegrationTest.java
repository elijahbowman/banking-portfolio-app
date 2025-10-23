package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.TestcontainersConfiguration;
import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transfer;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestcontainersConfiguration.class)
class AccountServiceIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    @Commit
    void setUp() {
        Account fromAccount = new Account();
        fromAccount.setAccountId("account1");
        fromAccount.setAccountNumber("number1");
        fromAccount.setCustomerName("name1");
        fromAccount.setBalance(new BigDecimal("1000.00"));
        accountRepository.saveAndFlush(fromAccount);

        Account toAccount = new Account();
        toAccount.setAccountId("account2");
        toAccount.setAccountNumber(("number2"));
        toAccount.setCustomerName("name2");
        toAccount.setBalance(new BigDecimal("500.00"));
        accountRepository.saveAndFlush(toAccount);
    }

    @Test
    void handleTransferEvent_ValidEvent_CreatesAndProcessesTransfer() {
        // Arrange
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transferId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "200.00");
        event.put("status", "PENDING");

        // Act
        kafkaTemplate.send("transfer-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Assert
        Transfer updatedTransfer = transferRepository.findById("transfer1").orElseThrow();
        assertEquals("COMPLETED", updatedTransfer.getStatus());
        assertEquals(new BigDecimal("800.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("700.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }

    @Test
    @Commit
    void handleTransferEvent_InsufficientBalance_TriggersRollback() {
        // Arrange
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transferId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "2000.00");
        event.put("status", "PENDING");

        // Act
        kafkaTemplate.send("transfer-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Assert
        Transfer updatedTransfer = transferRepository.findById("transfer1").orElseThrow();
        assertEquals("PENDING", updatedTransfer.getStatus()); // Not COMPLETED due to failure
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("500.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }

    @Test
    @Commit
    void handleRollback_ValidEvent_ReversesTransfer() {
        // Arrange
        Transfer transfer = new Transfer();
        transfer.setTransferId("transfer1");
        transfer.setFromAccountId("account1");
        transfer.setToAccountId("account2");
        transfer.setAmount(new BigDecimal("200.00"));
        transfer.setStatus("COMPLETED");
        transferRepository.save(transfer);

        Account fromAccount = accountRepository.findById("account1").orElseThrow();
        fromAccount.setBalance(new BigDecimal("800.00")); // After transfer
        accountRepository.save(fromAccount);

        Account toAccount = accountRepository.findById("account2").orElseThrow();
        toAccount.setBalance(new BigDecimal("700.00")); // After transfer
        accountRepository.save(toAccount);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transferId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "200.00");

        // Act
        kafkaTemplate.send("rollback-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Assert
        Transfer updatedTransfer = transferRepository.findById("transfer1").orElseThrow();
        assertEquals("ROLLED_BACK", updatedTransfer.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("500.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }
}