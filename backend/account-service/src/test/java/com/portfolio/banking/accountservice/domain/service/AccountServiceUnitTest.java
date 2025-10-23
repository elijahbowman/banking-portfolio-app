package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transaction;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class AccountServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionProcessor transactionProcessor;

    @Mock
    private TransactionRollbackHandler rollbackHandler;

    @InjectMocks
    private AccountService accountService;

    private Transaction transaction;
    private Account account;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setTransactionId("transaction1");
        transaction.setAmount(new BigDecimal("200.00"));
        transaction.setStatus("PENDING");

        account = new Account();
        account.setAccountId("account1");
        account.setAccountNumber("number1");
        account.setCustomerName("name1");
        account.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void handleTransferEvent_SuccessfulTransfer_CallsProcessor() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transactionId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "200.00");
        event.put("transactionType", "TRANSFER");
        event.put("status", "PENDING");

        transaction.setTransactionType("TRANSFER");
        transaction.setFromAccountId("account1");
        transaction.setToAccountId("account2");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionProcessor.processTransfer("transfer1", "account1", "account2", new BigDecimal("200.00")))
                .thenReturn(Map.of("transactionId", "transfer1", "status", "COMPLETED", "transactionType", "TRANSFER"));

        accountService.handleTransactionEvent(event);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transactionProcessor, times(1)).processTransfer("transfer1", "account1", "account2", new BigDecimal("200.00"));
        verifyNoInteractions(rollbackHandler);
    }

    @Test
    void handleDepositEvent_SuccessfulDeposit_CallsProcessor() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DEPOSIT_INITIATED");
        event.put("transactionId", "deposit1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "DEPOSIT");
        event.put("status", "PENDING");

        transaction.setTransactionType("DEPOSIT");
        transaction.setAccountId("account1");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionProcessor.processDeposit("deposit1", "account1", new BigDecimal("100.00")))
                .thenReturn(Map.of("transactionId", "deposit1", "status", "COMPLETED", "transactionType", "DEPOSIT"));

        accountService.handleTransactionEvent(event);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transactionProcessor, times(1)).processDeposit("deposit1", "account1", new BigDecimal("100.00"));
        verifyNoInteractions(rollbackHandler);
    }

    @Test
    void handleWithdrawalEvent_SuccessfulWithdrawal_CallsProcessor() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WITHDRAWAL_INITIATED");
        event.put("transactionId", "withdrawal1");
        event.put("accountId", "account1");
        event.put("amount", "100.00");
        event.put("transactionType", "WITHDRAWAL");
        event.put("status", "PENDING");

        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAccountId("account1");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionProcessor.processWithdrawal("withdrawal1", "account1", new BigDecimal("100.00")))
                .thenReturn(Map.of("transactionId", "withdrawal1", "status", "COMPLETED", "transactionType", "WITHDRAWAL"));

        accountService.handleTransactionEvent(event);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transactionProcessor, times(1)).processWithdrawal("withdrawal1", "account1", new BigDecimal("100.00"));
        verifyNoInteractions(rollbackHandler);
    }

    @Test
    void handleTransferEvent_InsufficientBalance_LogsError() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSFER_INITIATED");
        event.put("transactionId", "transfer1");
        event.put("fromAccountId", "account1");
        event.put("toAccountId", "account2");
        event.put("amount", "2000.00");
        event.put("transactionType", "TRANSFER");
        event.put("status", "PENDING");

        transaction.setTransactionType("TRANSFER");
        transaction.setFromAccountId("account1");
        transaction.setToAccountId("account2");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionProcessor.processTransfer("transfer1", "account1", "account2", new BigDecimal("2000.00")))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        accountService.handleTransactionEvent(event);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transactionProcessor, times(1)).processTransfer("transfer1", "account1", "account2", new BigDecimal("2000.00"));
        verifyNoInteractions(rollbackHandler);
    }

    @Test
    void handleWithdrawalEvent_InsufficientBalance_LogsError() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WITHDRAWAL_INITIATED");
        event.put("transactionId", "withdrawal1");
        event.put("accountId", "account1");
        event.put("amount", "2000.00");
        event.put("transactionType", "WITHDRAWAL");
        event.put("status", "PENDING");

        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAccountId("account1");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionProcessor.processWithdrawal("withdrawal1", "account1", new BigDecimal("2000.00")))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        accountService.handleTransactionEvent(event);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transactionProcessor, times(1)).processWithdrawal("withdrawal1", "account1", new BigDecimal("2000.00"));
        verifyNoInteractions(rollbackHandler);
    }

    @Test
    void handleRollbackEvent_CallsRollbackHandler() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transactionId", "transaction1");
        event.put("transactionType", "TRANSFER");

        accountService.handleRollbackEvent(event);

        verify(rollbackHandler, times(1)).handleRollback(event);
        verifyNoInteractions(transactionRepository, transactionProcessor);
    }

    @Test
    void handleUnknownEventType_LogsWarning() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "UNKNOWN_EVENT");
        event.put("transactionId", "transaction1");

        accountService.handleTransactionEvent(event);

        verifyNoInteractions(transactionRepository, transactionProcessor, rollbackHandler);
    }

    @Test
    void getBalance_ValidAccount_ReturnsBalance() {
        when(accountRepository.findById("account1")).thenReturn(Optional.of(account));

        BigDecimal balance = accountService.getBalance("account1");

        assertEquals(new BigDecimal("1000.00"), balance);
        verify(accountRepository, times(1)).findById("account1");
    }

    @Test
    void getBalance_NonExistentAccount_ThrowsException() {
        when(accountRepository.findById("nonexistent")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getBalance("nonexistent")
        );
        assertEquals("Account not found: nonexistent", exception.getMessage());
        verify(accountRepository, times(1)).findById("nonexistent");
    }

    @Test
    void getBalance_EmptyAccountId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getBalance("")
        );
        assertEquals("Account ID must not be null", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
    }
}