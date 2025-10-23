package com.portfolio.banking.accountservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.banking.accountservice.domain.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AccountControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void shouldReturnBalanceForValidAccount() throws Exception {
        when(accountService.getBalance("account1"))
                .thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/v1/accounts/balance?accountId=account1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("account1"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void shouldReturn400ForInvalidAccountId() throws Exception {
        when(accountService.getBalance("nonexistent"))
                .thenThrow(new IllegalArgumentException("Account not found: nonexistent"));

        mockMvc.perform(get("/api/v1/accounts/balance?accountId=nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account not found: nonexistent"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400ForEmptyAccountId() throws Exception {
        when(accountService.getBalance(""))
                .thenThrow(new IllegalArgumentException("Account ID must not be null"));

        mockMvc.perform(get("/api/v1/accounts/balance?accountId=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account ID must not be null"))
                .andExpect(jsonPath("$.status").value(400));
    }

}