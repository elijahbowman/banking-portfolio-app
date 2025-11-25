package com.portfolio.banking.cardservice.lambda;

import com.portfolio.banking.cardservice.domain.entity.Card;
import com.portfolio.banking.cardservice.domain.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

@Component
public class IssueVCNFunction implements Function<Map<String, Object>, Card> {

    @Autowired
    private CardService cardService;

    @Override
    public Card apply(Map<String, Object> input) {
        String accountId = (String) input.get("accountId");
        String limitStr = (String) input.get("limit");
        BigDecimal limit = new BigDecimal(limitStr);

        return cardService.issueVCN(accountId, limit);
    }

    // Required for Spring to detect this as a function bean
    @Bean
    public Function<Map<String, Object>, Card> issueVCN() {
        return this;
    }
}