package com.portfolio.banking.cardservice.domain.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDbBean
@Data
public class Card {
    private String cardId;
    private String accountId;
    private BigDecimal usageLimit;
    private String vcn;  // Virtual Card Number
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    @DynamoDbPartitionKey
    public String getCardId() {
        return cardId;
    }
}