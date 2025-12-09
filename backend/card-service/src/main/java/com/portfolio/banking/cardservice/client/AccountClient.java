package com.portfolio.banking.cardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/balance")
    BigDecimal getBalance(@RequestBody String accountId);
}