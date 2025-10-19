package com.portfolio.banking.bankingservice.domain.service;

import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {"transfer-events",  "rollback-events"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092"
//                "port=9092",
//                "auto.create.topics.enable=true"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BankingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private BankingService bankingService;

    @Autowired
    private AccountRepository accountRepository;

//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @BeforeEach
    void setUp() {
//        embeddedKafkaBroker.addTopicsIfNotExists("transfer-events", "rollback-events");

        Account fromAccount = new Account();
        fromAccount.setAccountId("account1");
        fromAccount.setAccountNumber("number1");
        fromAccount.setCustomerName("name1");
        fromAccount.setBalance(new BigDecimal("1000.00"));
        accountRepository.save(fromAccount);

        Account toAccount = new Account();
        toAccount.setAccountId("account2");
        toAccount.setAccountNumber(("number2"));
        toAccount.setCustomerName("name2");
        toAccount.setBalance(new BigDecimal("500.00"));
        accountRepository.save(toAccount);
    }

    @Test
    void initiateTransfer_SuccessfulInitiation_PublishesEvent() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("200.00");

        // Act
        Map<String, Object> response = bankingService.initiateTransfer(fromAccountId, toAccountId, amount);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("transferId"));
        assertEquals("PENDING", response.get("status"));
    }

    @Test
    void initiateTransfer_InsufficientBalance_TriggersRollback() {
        // Arrange
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("2000.00");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bankingService.initiateTransfer(fromAccountId, toAccountId, amount));
        assertEquals("Insufficient balance", exception.getMessage());
    }
}