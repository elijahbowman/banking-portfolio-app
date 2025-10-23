package com.portfolio.banking.bankingservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.banking.bankingservice.domain.exception.BalanceFetchException;
import com.portfolio.banking.bankingservice.domain.service.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankingControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankingService bankingService;

    @Test
    void shouldProcessValidTransfer() throws Exception {
        Map<String, Object> request = Map.of(
                "fromAccountId", "account1",
                "toAccountId", "account2",
                "amount", "200.00"
        );
        Map<String, Object> response = Map.of(
                "transactionId", "transfer1",
                "status", "PENDING"
        );

        when(bankingService.initiateTransfer("account1", "account2", new BigDecimal("200.00")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/banking/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("transfer1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldProcessValidDeposit() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", "account1",
                "amount", "100.00"
        );
        Map<String, Object> response = Map.of(
                "transactionId", "deposit1",
                "status", "PENDING"
        );

        when(bankingService.initiateDeposit("account1", new BigDecimal("100.00")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/banking/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("deposit1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldProcessValidWithdrawal() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", "account1",
                "amount", "100.00"
        );
        Map<String, Object> response = Map.of(
                "transactionId", "withdrawal1",
                "status", "PENDING"
        );

        when(bankingService.initiateWithdrawal("account1", new BigDecimal("100.00")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/banking/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("withdrawal1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnBalanceForValidAccount() throws Exception {
        when(bankingService.getBalance("account1"))
                .thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/v1/banking/accounts/account1/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("account1"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void shouldHandleInvalidAmountForTransfer() throws Exception {
        Map<String, Object> request = Map.of(
                "fromAccountId", "account1",
                "toAccountId", "account2",
                "amount", "0"
        );

        when(bankingService.initiateTransfer(anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        mockMvc.perform(post("/api/v1/banking/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be positive"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleInvalidAmountForDeposit() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", "account1",
                "amount", "0"
        );

        when(bankingService.initiateDeposit(anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        mockMvc.perform(post("/api/v1/banking/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be positive"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleInvalidAmountForWithdrawal() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", "account1",
                "amount", "0"
        );

        when(bankingService.initiateWithdrawal(anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        mockMvc.perform(post("/api/v1/banking/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be positive"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400ForMissingAccountIdInDeposit() throws Exception {
        Map<String, Object> request = Map.of(
                "amount", "100.00"
        );

        mockMvc.perform(post("/api/v1/banking/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account ID must not be null"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400ForMissingAmountInWithdrawal() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", "account1"
        );

        mockMvc.perform(post("/api/v1/banking/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must not be null"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleAccountServiceDownForBalance() throws Exception {
        when(bankingService.getBalance("account1"))
                .thenThrow(new BalanceFetchException("Failed to fetch balance for account: account1"));

        mockMvc.perform(get("/api/v1/banking/accounts/account1/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to fetch balance for account: account1"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/banking/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.service").value("BankingService"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void shouldReturnInfo() throws Exception {
        mockMvc.perform(get("/api/v1/banking/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("BankingService"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.kafkaEnabled").value(true));
    }
}