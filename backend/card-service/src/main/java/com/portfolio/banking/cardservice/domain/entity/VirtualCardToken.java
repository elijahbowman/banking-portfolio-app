package com.portfolio.banking.cardservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.math.BigDecimal;
import java.time.Instant;

@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualCardToken {
    private String tokenId;
    private String realCardId;
    private String vcn;
    private BigDecimal spendLimit;
    private Instant expiresAt;
    private Instant createdAt;
    @Builder.Default
    private String status = "ACTIVE";

    @DynamoDbPartitionKey
    public String getTokenId() { return tokenId; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"RealCardIdIndex"})
    public String getRealCardId() { return realCardId; }

}