package com.portfolio.banking.accountservice.domain.service;

import com.portfolio.banking.accountservice.domain.entity.Account;
import com.portfolio.banking.accountservice.domain.entity.Transfer;
import com.portfolio.banking.accountservice.repository.AccountRepository;
import com.portfolio.banking.accountservice.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AccountService accountService;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setAccountId("account1");
        fromAccount.setBalance(new BigDecimal("1000.00"));

        toAccount = new Account();
        toAccount.setAccountId("account2");
        toAccount.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void processTransfer_SuccessfulTransfer_ReturnsTransferResponse() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("200.00");

        Transfer savedTransfer = new Transfer();
        savedTransfer.setTransferId("transfer1");
        savedTransfer.setFromAccountId(fromAccountId);
        savedTransfer.setToAccountId(toAccountId);
        savedTransfer.setAmount(amount);
        savedTransfer.setStatus("COMPLETED");

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);
        when(kafkaTemplate.send(anyString(), anyString(), any(Map.class))).thenReturn(null);

        // Act
        Map<String, Object> response = accountService.processTransfer(fromAccountId, toAccountId, amount);

        // Assert
        assertNotNull(response);
        assertEquals("transfer1", response.get("transferId"));
        assertEquals(new BigDecimal("800.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(kafkaTemplate, times(1)).send(eq("transfer-events"), anyString(), any(Map.class));
    }

    @Test
    void processTransfer_InsufficientBalance_ThrowsIllegalStateException() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("2000.00"); // More than balance

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                accountService.processTransfer(fromAccountId, toAccountId, amount));
        assertEquals("Insufficient balance", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(Map.class));
    }

    @Test
    void processTransfer_SameAccount_ThrowsIllegalArgumentException() {
        // Arrange
        String accountId = "account1";
        BigDecimal amount = new BigDecimal("200.00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                accountService.processTransfer(accountId, accountId, amount));
        assertEquals("Invalid accounts", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void processTransfer_NegativeAmount_ThrowsIllegalArgumentException() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("-200.00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                accountService.processTransfer(fromAccountId, toAccountId, amount));
        assertEquals("Amount must be positive", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void processTransfer_AccountNotFound_ThrowsIllegalArgumentException() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("200.00");

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                accountService.processTransfer(fromAccountId, toAccountId, amount));
        assertEquals("Account not found: account1", exception.getMessage());
        verify(accountRepository, times(1)).findById(fromAccountId);
        verify(accountRepository, never()).findById(toAccountId);
    }
}