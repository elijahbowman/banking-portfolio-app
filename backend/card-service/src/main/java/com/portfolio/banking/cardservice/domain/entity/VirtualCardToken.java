package com.portfolio.banking.cardservice.domain.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    @Builder.Default
    private String status = "ACTIVE";

    @DynamoDbPartitionKey
    public String getTokenId() { return tokenId; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"RealCardIdIndex"})
    public String getRealCardId() { return realCardId; }

}