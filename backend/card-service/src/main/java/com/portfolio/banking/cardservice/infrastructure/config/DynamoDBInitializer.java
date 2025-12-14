package com.portfolio.banking.cardservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

@Configuration
@Profile("lambda")
public class DynamoDBInitializer {
    private final DynamoDbClient client;
    private final DynamoDbEnhancedClient enhancedClient;

    public DynamoDBInitializer(DynamoDbClient client, DynamoDbEnhancedClient enhancedClient) {
        this.client = client;
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    public void initializeClient() {
        client.describeTable(DescribeTableRequest.builder().tableName("Cards").build());

        client.describeTable(DescribeTableRequest.builder().tableName("VirtualCardTokens").build());
    }
}