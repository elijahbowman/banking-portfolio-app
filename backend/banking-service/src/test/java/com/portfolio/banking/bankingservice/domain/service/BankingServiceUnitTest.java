package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BankingServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BankingService bankingService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAccountId("TEST001");
        testAccount.setAccountNumber("ACC1001");
        testAccount.setCustomerName("Test Customer");
        testAccount.setBalance(BigDecimal.valueOf(1000.00));
        testAccount.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldProcessValidDepositForExistingAccount() {
        when(accountRepository.findById("TEST001")).thenReturn(Optional.of(testAccount));

        Account savedAccount = new Account();
        savedAccount.setAccountId("TEST001");
        savedAccount.setAccountNumber("ACC1001");
        savedAccount.setCustomerName("Test Customer");
        savedAccount.setBalance(BigDecimal.valueOf(1500.00));
        savedAccount.setCreatedAt(testAccount.getCreatedAt());
        savedAccount.setUpdatedAt(LocalDateTime.now());

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Map<String, Object> result = bankingService.processDeposit("TEST001", BigDecimal.valueOf(500.00));

        assertEquals("COMPLETED", result.get("status"));
        assertEquals("TEST001", result.get("accountId"));
        assertEquals(500.00, ((BigDecimal) result.get("amount")).doubleValue());
        assertEquals(1000.00, ((BigDecimal) result.get("oldBalance")).doubleValue());
        assertEquals(1500.00, ((BigDecimal) result.get("newBalance")).doubleValue());
        assertNotNull(result.get("transactionId"));

        verify(accountRepository, times(1)).findById("TEST001");
        verify(accountRepository, times(1)).save(testAccount);
        verify(kafkaTemplate, times(1)).send(eq("deposit-events"), eq("TEST001"), any(Map.class));
    }

    @Test
    void shouldCreateNewAccountForFirstDeposit() {
        when(accountRepository.findById("NEW001")).thenReturn(Optional.empty());

        Account savedAccount = new Account();
        savedAccount.setAccountId("NEW001");
        savedAccount.setAccountNumber("ACCNEW1");
        savedAccount.setCustomerName("Portfolio Customer NEW001");
        savedAccount.setBalance(BigDecimal.valueOf(0.00));  // After deposit
        savedAccount.setCreatedAt(LocalDateTime.now());
        savedAccount.setUpdatedAt(LocalDateTime.now());

        when(accountRepository.save(any(Account.class))).
                thenReturn(savedAccount)
                .thenAnswer(invocation -> {
                    savedAccount.setBalance(BigDecimal.valueOf(100.00));
                    return savedAccount;
                });

        Map<String, Object> result = bankingService.processDeposit("NEW001", BigDecimal.valueOf(100.00));

        assertEquals("COMPLETED", result.get("status"));
        assertEquals("NEW001", result.get("accountId"));
        System.out.println("result: ");
        System.out.println(result);
        assertEquals(0.00, ((BigDecimal) result.get("oldBalance")).doubleValue());
        assertEquals(100.00, ((BigDecimal) result.get("newBalance")).doubleValue());
        assertNotNull(result.get("transactionId"));

        verify(accountRepository, times(1)).findById("NEW001");
        verify(accountRepository, times(1)).save(argThat(account ->
                "NEW001".equals(account.getAccountId()) &&
                        BigDecimal.ZERO.equals(account.getBalance())
        ));
        verify(kafkaTemplate, times(1)).send(eq("deposit-events"), eq("NEW001"), any(Map.class));
    }

    @Test
    void shouldRejectZeroDepositAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.processDeposit("TEST001", BigDecimal.ZERO)
        );
        assertEquals("Deposit amount must be positive", exception.getMessage());

        verify(accountRepository, never()).findById(anyString());
        verify(accountRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectNullDepositAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.processDeposit("TEST001", null)
        );
        assertEquals("Deposit amount must be positive", exception.getMessage());

        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectEmptyAccountId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.processDeposit("", BigDecimal.valueOf(100.00))
        );
        assertEquals("Account ID is required", exception.getMessage());

        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }
}