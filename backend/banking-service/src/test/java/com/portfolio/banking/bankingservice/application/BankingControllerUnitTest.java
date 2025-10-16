package com.portfolio.banking.bankingservice.application;

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
class BankingControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankingService bankingService;

    @Test
    void shouldProcessValidDeposit() throws Exception {
        Map<String, Object> successResponse = Map.of(
                "transactionId", "test-123",
                "accountId", "TEST001",
                "amount", BigDecimal.valueOf(100.00),
                "oldBalance", BigDecimal.ZERO,
                "newBalance", BigDecimal.valueOf(100.00),
                "status", "COMPLETED",
                "timestamp", "2025-09-20T08:00:00"
        );

        when(bankingService.processDeposit(eq("TEST001"), any(BigDecimal.class)))
                .thenReturn(successResponse);

        mockMvc.perform(post("/api/v1/banking/deposits")
                        .param("accountId", "TEST001")
                        .param("amount", "100.00")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountId").value("TEST001"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.newBalance").value(100.00));
    }

    @Test
    void shouldHandleInvalidAmount() throws Exception {
        when(bankingService.processDeposit(anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("Deposit amount must be positive"));

        mockMvc.perform(post("/api/v1/banking/deposits")
                        .param("accountId", "INVALID001")
                        .param("amount", "0")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error").value("Deposit amount must be positive"));
    }

    @Test
    void shouldReturn400ForMissingAccountId() throws Exception {
        mockMvc.perform(post("/api/v1/banking/deposits")
                        .param("amount", "100.00")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMissingAmount() throws Exception {
        mockMvc.perform(post("/api/v1/banking/deposits")
                        .param("accountId", "MISSING001")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest());
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