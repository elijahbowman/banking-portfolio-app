package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.domain.entity.Card;
import com.portfolio.banking.cardservice.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.math.BigDecimal;

@SpringBootTest
@Testcontainers
class CardServiceIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @DynamicPropertySource
    static void registerLocalStackProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.dynamodb.endpoint",
                localStack::getEndpoint);
    }

    private void createCardTableIfNotExists(DynamoDbClient client) {
        final String tableName = "Cards";

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("cardId")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName("cardId")
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                .build();

        try {
            client.createTable(request);
            System.out.println("Created table: " + tableName);

            DynamoDbWaiter waiter = client.waiter();
            waiter.waitUntilTableExists(r -> r.tableName(tableName));

        } catch (ResourceInUseException e) {
            System.out.println("Table " + tableName + " already exists. Skipping creation.");

            DynamoDbWaiter waiter = client.waiter();
            waiter.waitUntilTableExists(r -> r.tableName(tableName));

        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());
            throw e;
        }
    }

    @BeforeEach
    public void setUp() {
        createCardTableIfNotExists(dynamoDbClient);
    }

    @Test
    void testCardCreation() {
        Card card = new Card();
        card.setCardId("test-123");
        card.setAccountId("account-123");
        card.setUsageLimit(new BigDecimal("1000"));
        card.setVcn("VCN-TEST");
        cardRepository.save(card);

        Card saved = cardRepository.findById("test-123");
        assert saved != null;
        assert saved.getAccountId().equals("account-123");
    }
}