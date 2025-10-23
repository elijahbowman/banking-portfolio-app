package com.portfolio.banking.accountservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompletedEvent(String transactionId, String transactionType, String fromAccountId,
                                      String toAccountId, String accountId, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", transactionType + "_COMPLETED");
        event.put("transactionId", transactionId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("accountId", accountId);
        event.put("amount", amount);
        event.put("transactionType", transactionType);
        event.put("status", "COMPLETED");

        kafkaTemplate.send("transaction-events", transactionId, event);
        log.info("Published {} completed event: {}", transactionType, event);
    }

    public void publishRollbackEvent(String transactionId, String transactionType, String fromAccountId,
                                     String toAccountId, String accountId, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ROLLBACK");
        event.put("transactionId", transactionId);
        event.put("fromAccountId", fromAccountId);
        event.put("toAccountId", toAccountId);
        event.put("accountId", accountId);
        event.put("amount", amount);
        event.put("transactionType", transactionType);

        kafkaTemplate.send("rollback-events", transactionId, event);
        log.info("Published rollback event: {}", event);
    }
}