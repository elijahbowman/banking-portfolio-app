package com.portfolio.banking.cardservice.infrastructure.config;

import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;

@Configuration
@Profile("lambda-local")
public class localStackDynamoDBInitializer {
    private final DynamoDbClient client;
    private final DynamoDbEnhancedClient enhancedClient;

    public localStackDynamoDBInitializer(DynamoDbClient client, DynamoDbEnhancedClient enhancedClient) {
        this.client = client;
        this.enhancedClient = enhancedClient;
    }

    @PostConstruct
    public void initializeTables_createIfNotExists() {
        try {
            client.describeTable(DescribeTableRequest.builder().tableName("Cards").build());
        } catch (ResourceNotFoundException e) {
            enhancedClient.table("Cards", TableSchema.fromBean(RealCard.class))
                    .createTable(CreateTableEnhancedRequest.builder()
                            .provisionedThroughput(ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build())
                            .build());
        }

        try {
            client.describeTable(DescribeTableRequest.builder().tableName("VirtualCardTokens").build());
        } catch (ResourceNotFoundException e) {
            enhancedClient.table("VirtualCardTokens", TableSchema.fromBean(VirtualCardToken.class))
                    .createTable(CreateTableEnhancedRequest.builder()
                            .provisionedThroughput(ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build())
                            .globalSecondaryIndices(
                                    List.of(
                                            EnhancedGlobalSecondaryIndex.builder()
                                                    .indexName("RealCardIdIndex")
                                                    .projection(Projection.builder()
                                                            .projectionType(ProjectionType.ALL)
                                                            .build())
                                                    .provisionedThroughput(ProvisionedThroughput.builder()
                                                            .readCapacityUnits(5L)
                                                            .writeCapacityUnits(5L)
                                                            .build())
                                                    .build()
                                    )
                            )
                            .build());
        }
    }
}