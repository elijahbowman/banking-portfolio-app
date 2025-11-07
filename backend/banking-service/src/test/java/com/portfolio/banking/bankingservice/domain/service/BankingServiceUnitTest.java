package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.domain.exception.BalanceFetchException;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BankingService bankingService;

    private Account fromAccount;
    private Account toAccount;
    private String balanceUrl;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        fromAccount = new Account();
        fromAccount.setAccountId("account1");
        fromAccount.setAccountNumber("number1");
        fromAccount.setCustomerName("name1");
        fromAccount.setBalance(new BigDecimal("1000.00"));
        accountRepository.saveAndFlush(fromAccount);

        toAccount = new Account();
        toAccount.setAccountId("account2");
        toAccount.setAccountNumber(("number2"));
        toAccount.setCustomerName("name2");
        toAccount.setBalance(new BigDecimal("500.00"));
        accountRepository.saveAndFlush(toAccount);

        balanceUrl = "http://account-service:8081/api/v1/accounts/balance?accountId=account1";

//        // Set accountServiceUrl to empty to match the expected URL
//        Field field = BankingService.class.getDeclaredField("accountServiceUrl");
//        field.setAccessible(true);
//        field.set(bankingService, "");

        // Set accountServiceUrl to empty
        try {
            Field urlField = BankingService.class.getDeclaredField("accountServiceUrl");
            urlField.setAccessible(true);
            urlField.set(bankingService, "http://account-service:8081");
        } catch (NoSuchFieldException e) {
            throw e;
        }

        // Set restTemplate to the mocked instance
        try {
            Field restTemplateField = BankingService.class.getDeclaredField("restTemplate");
            restTemplateField.setAccessible(true);
            restTemplateField.set(bankingService, restTemplate);
        } catch (NoSuchFieldException e) {
            System.err.println("Field restTemplate not found: " + e.getMessage());
            throw e;
        }
    }

    @Test
    void shouldInitiateValidTransfer() {
        when(accountRepository.findById("account1")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("account2")).thenReturn(Optional.of(toAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any(Map.class))).thenReturn(null);

        Map<String, Object> result = bankingService.initiateTransfer("account1", "account2", new BigDecimal("200.00"));

        assertNotNull(result);
        assertTrue(result.containsKey("transactionId"));
        assertEquals("PENDING", result.get("status"));
        verify(accountRepository, times(1)).findById("account1");
        verify(accountRepository, times(1)).findById("account2");
        verify(kafkaTemplate, times(1)).send(eq("transaction-events"), anyString(), any(Map.class));
    }

    @Test
    void shouldInitiateValidDeposit() {
        when(accountRepository.findById("account1")).thenReturn(Optional.of(fromAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any(Map.class))).thenReturn(null);

        Map<String, Object> result = bankingService.initiateDeposit("account1", new BigDecimal("100.00"));

        assertNotNull(result);
        assertTrue(result.containsKey("transactionId"));
        assertEquals("PENDING", result.get("status"));
        verify(accountRepository, times(2)).findById("account1");
        verify(kafkaTemplate, times(1)).send(eq("transaction-events"), anyString(), any(Map.class));
    }

    @Test
    void shouldInitiateValidWithdrawal() {
        when(accountRepository.findById("account1")).thenReturn(Optional.of(fromAccount));
        when(kafkaTemplate.send(anyString(), anyString(), any(Map.class))).thenReturn(null);

        Map<String, Object> result = bankingService.initiateWithdrawal("account1", new BigDecimal("100.00"));

        assertNotNull(result);
        assertTrue(result.containsKey("transactionId"));
        assertEquals("PENDING", result.get("status"));
        verify(accountRepository, times(1)).findById("account1");
        verify(kafkaTemplate, times(1)).send(eq("transaction-events"), anyString(), any(Map.class));
    }

    @Test
    void shouldReturnBalanceForValidAccount() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accountId", "account1");
        responseBody.put("balance", "1000.00");

        when(restTemplate.exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        BigDecimal balance = bankingService.getBalance("account1");

        assertEquals(new BigDecimal("1000.00"), balance);
        verify(restTemplate, times(1)).exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void shouldThrowExceptionForMissingBalanceKey() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accountId", "account1");
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance("account1")
        );
        assertEquals("Failed to fetch balance for account: account1", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForFailedBalanceRequest() {
        when(restTemplate.exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance("account1")
        );
        assertEquals("Failed to fetch balance for account: account1", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForInvalidBalanceFormat() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accountId", "account1");
        responseBody.put("balance", "invalid");
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance("account1")
        );
        assertEquals("Invalid balance format for account: account1", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq(balanceUrl),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForNullAccountId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.getBalance(null)
        );
        assertEquals("Account ID must not be null", exception.getMessage());
        verify(restTemplate, never()).exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForEmptyAccountId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.getBalance("")
        );
        assertEquals("Account ID must not be null", exception.getMessage());
        verify(restTemplate, never()).exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForNullResponseBody() {
        when(restTemplate.exchange(
                contains("accountId=account1"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance("account1")
        );
        assertEquals("Response body is null for account: account1", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                contains("accountId=account1"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowExceptionForMissingBalanceInResponse() {
        when(restTemplate.exchange(
                contains("accountId=account1"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(Map.of("accountId", "account1")));

        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance("account1")
        );
        assertEquals("Failed to fetch balance for account: account1", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                contains("accountId=account1"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldRejectZeroTransferAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.initiateTransfer("account1", "account2", BigDecimal.ZERO)
        );
        assertEquals("Amount must be positive", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectNullDepositAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.initiateDeposit("account1", null)
        );
        assertEquals("Amount must be positive", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectEmptyAccountIdForWithdrawal() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.initiateWithdrawal("", new BigDecimal("100.00"))
        );
        assertEquals("Account ID must not be null", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectNonExistentAccountForTransfer() {
        when(accountRepository.findById("account1")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.initiateTransfer("account1", "account2", new BigDecimal("200.00"))
        );
        assertEquals("Account not found: account1", exception.getMessage());
        verify(accountRepository, times(1)).findById("account1");
        verify(accountRepository, never()).findById("account2");
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldRejectSameAccountForTransfer() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankingService.initiateTransfer("account1", "account1", new BigDecimal("200.00"))
        );
        assertEquals("Invalid accounts", exception.getMessage());
        verify(accountRepository, never()).findById(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }
}