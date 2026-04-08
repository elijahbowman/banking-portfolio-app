package com.portfolio.banking.cardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/balance")
    AccountBalanceResponse getBalance(@RequestParam String accountId);
}