package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.TestcontainersConfiguration;
import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transaction;
import com.portfolio.banking.accountservice.domain.entity.Transfer;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransactionRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TestcontainersConfiguration.class)
class AccountServiceIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private AccountService accountService;

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
    void handleTransferEvent_ValidEvent_ProcessesTransfer() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transactionId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "200.00");
        event.put("transactionType", "TRANSFER");
        event.put("status", "PENDING");

        kafkaTemplate.send("transaction-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction transaction = transactionRepository.findById("transfer1").orElseThrow();
        assertEquals("COMPLETED", transaction.getStatus());
        assertEquals(new BigDecimal("800.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("700.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }

    @Test
    void handleTransferEvent_InsufficientBalance_TriggersRollback() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transactionId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "2000.00");
        event.put("transactionType", "TRANSFER");
        event.put("status", "PENDING");

        kafkaTemplate.send("transaction-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction transaction = transactionRepository.findById("transfer1").orElseThrow();
        assertEquals("PENDING", transaction.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("500.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }

    @Test
    void handleDepositEvent_ValidEvent_ProcessesDeposit() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DEPOSIT_INITIATED");
        event.put("transactionId", "deposit1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "DEPOSIT");
        event.put("status", "PENDING");

        kafkaTemplate.send("transaction-events", "deposit1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction transaction = transactionRepository.findById("deposit1").orElseThrow();
        assertEquals("COMPLETED", transaction.getStatus());
        assertEquals(new BigDecimal("1100.00"), accountRepository.findById("account1").orElseThrow().getBalance());
    }

    @Test
    void handleWithdrawalEvent_ValidEvent_ProcessesWithdrawal() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WITHDRAWAL_INITIATED");
        event.put("transactionId", "withdrawal1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "WITHDRAWAL");
        event.put("status", "PENDING");

        kafkaTemplate.send("transaction-events", "withdrawal1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction transaction = transactionRepository.findById("withdrawal1").orElseThrow();
        assertEquals("COMPLETED", transaction.getStatus());
        assertEquals(new BigDecimal("900.00"), accountRepository.findById("account1").orElseThrow().getBalance());
    }

    @Test
    void handleWithdrawalEvent_InsufficientBalance_TriggersRollback() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WITHDRAWAL_INITIATED");
        event.put("transactionId", "withdrawal1");
        event.put("accountId", "account1");
        event.put("amount", "2000.00");
        event.put("transactionType", "WITHDRAWAL");
        event.put("status", "PENDING");

        kafkaTemplate.send("transaction-events", "withdrawal1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction transaction = transactionRepository.findById("withdrawal1").orElseThrow();
        assertEquals("PENDING", transaction.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
    }

    @Test
    void handleRollback_ValidTransfer_ReversesTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("transfer1");
        transaction.setFromAccountId("account1");
        transaction.setToAccountId("account2");
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setTransactionType("TRANSFER");
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        Account fromAccount = accountRepository.findById("account1").orElseThrow();
        fromAccount.setBalance(new BigDecimal("800.00"));
        accountRepository.save(fromAccount);

        Account toAccount = accountRepository.findById("account2").orElseThrow();
        toAccount.setBalance(new BigDecimal("700.00"));
        accountRepository.save(toAccount);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transactionId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "200.00");
        event.put("transactionType", "TRANSFER");

        kafkaTemplate.send("rollback-events", "transfer1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction updatedTransaction = transactionRepository.findById("transfer1").orElseThrow();
        assertEquals("ROLLED_BACK", updatedTransaction.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
        assertEquals(new BigDecimal("500.00"), accountRepository.findById("account2").orElseThrow().getBalance());
    }

    @Test
    void handleRollback_ValidDeposit_ReversesTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("deposit1");
        transaction.setAccountId("account1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionType("DEPOSIT");
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        Account account = accountRepository.findById("account1").orElseThrow();
        account.setBalance(new BigDecimal("1100.00"));
        accountRepository.save(account);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transactionId", "deposit1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "DEPOSIT");

        kafkaTemplate.send("rollback-events", "deposit1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction updatedTransaction = transactionRepository.findById("deposit1").orElseThrow();
        assertEquals("ROLLED_BACK", updatedTransaction.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
    }

    @Test
    void handleRollback_ValidWithdrawal_ReversesTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("withdrawal1");
        transaction.setAccountId("account1");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        Account account = accountRepository.findById("account1").orElseThrow();
        account.setBalance(new BigDecimal("900.00"));
        accountRepository.save(account);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transactionId", "withdrawal1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "WITHDRAWAL");

        kafkaTemplate.send("rollback-events", "withdrawal1", event);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Transaction updatedTransaction = transactionRepository.findById("withdrawal1").orElseThrow();
        assertEquals("ROLLED_BACK", updatedTransaction.getStatus());
        assertEquals(new BigDecimal("1000.00"), accountRepository.findById("account1").orElseThrow().getBalance());
    }

    @Test
    void getBalance_ValidAccount_ReturnsBalance() {
        BigDecimal balance = accountService.getBalance("account1");
        assertEquals(new BigDecimal("1000.00"), balance);
    }

    @Test
    void getBalance_NonExistentAccount_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getBalance("nonexistent")
        );
        assertEquals("Account not found: nonexistent", exception.getMessage());
    }

    @Test
    void getBalance_EmptyAccountId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getBalance("")
        );
        assertEquals("Account ID must not be null", exception.getMessage());
    }
}