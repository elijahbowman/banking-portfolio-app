package com.portfolio.banking.cardservice.lambda;

import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.domain.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class IssueVCNFunction {

    private final CardService cardService;

    @Bean
    public Function<Message<Map<String, Object>>, Message<VirtualCardToken>> issueVCN() {
        return message -> {
            Map<String, Object> headers = message.getHeaders();

            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) headers.get("queryStringParameters");

            String realCardId = queryParams.get("realCardId");
            String limitStr = queryParams.get("limit");

            if (realCardId == null || limitStr == null) {
                throw new IllegalArgumentException("realCardId and limit are required");
            }

            BigDecimal limit = new BigDecimal(limitStr);
            VirtualCardToken token = cardService.issueVCN(realCardId, limit);

            return MessageBuilder
                    .withPayload(token)
                    .setHeader("Content-Type", "application/json")
                    .build();
        };
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<RealCard>> getRealCard() {
        return message -> {
            Map<String, Object> headers = message.getHeaders();
            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) headers.get("queryStringParameters");

            String accountId = queryParams.get("accountId");
            if (accountId == null) {
                throw new IllegalArgumentException("accountId required");
            }

            String realCardId = "REAL#" + accountId;
            RealCard card = cardService.getRealCard(realCardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            return MessageBuilder.withPayload(card).setHeader("Content-Type", "application/json").build();
        };
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<List<VirtualCardToken>>> getVCNs() {
        return message -> {
            Map<String, Object> headers = message.getHeaders();
            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) headers.get("queryStringParameters");

            String realCardId = queryParams.get("realCardId");
            if (realCardId == null) {
                throw new IllegalArgumentException("realCardId required");
            }

            List<VirtualCardToken> tokens = cardService.getVCNsForRealCard(realCardId);

            return MessageBuilder.withPayload(tokens).setHeader("Content-Type", "application/json").build();
        };
    }
}