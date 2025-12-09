package com.portfolio.banking.cardservice.domain.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealCard {
    private String cardId;           // PK: "REAL#acc123"
    private String accountId;        // FK to account
    private String cardHolderName;
    private String cardNumber;       // Masked in UI: "4532 **** **** 9012"
    private String status;           // ACTIVE, BLOCKED
    private String expiryDate;       // MM/YY

    @DynamoDbPartitionKey
    public String getCardId() { return cardId; }
}