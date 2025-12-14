package com.portfolio.banking.cardservice.client;

import java.math.BigDecimal;

public record AccountBalanceResponse(BigDecimal balance, String accountId) { }
