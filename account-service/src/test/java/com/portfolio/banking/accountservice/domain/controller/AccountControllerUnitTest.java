package com.portfolio.banking.accountservice.domain.controller;

import com.portfolio.banking.accountservice.application.AccountController;
import com.portfolio.banking.accountservice.domain.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void transfer_SuccessfulTransfer_ReturnsOkWithTransferId() throws Exception {
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("200.00");

        Map<String, Object> response = new HashMap<>();
        response.put("transferId", "transfer1");

        when(accountService.processTransfer(fromAccountId, toAccountId, amount)).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .param("fromAccountId", fromAccountId)
                        .param("toAccountId", toAccountId)
                        .param("amount", amount.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value("transfer1"));
    }

    @Test
    void transfer_InvalidAmount_ThrowsBadRequest() throws Exception {
        String fromAccountId = "account1";
        String toAccountId = "account2";
        String invalidAmount = "invalid";

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .param("fromAccountId", fromAccountId)
                        .param("toAccountId", toAccountId)
                        .param("amount", invalidAmount)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ServiceThrowsIllegalStateException_ReturnsBadRequest() throws Exception {
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("2000.00");

        when(accountService.processTransfer(anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .param("fromAccountId", fromAccountId)
                        .param("toAccountId", toAccountId)
                        .param("amount", amount.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void transfer_MissingParameters_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .param("fromAccountId", "account1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}