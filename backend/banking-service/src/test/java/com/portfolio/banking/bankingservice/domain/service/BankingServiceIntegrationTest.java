package com.portfolio.banking.bankingservice.domain.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.portfolio.banking.bankingservice.domain.entity.Account;
import com.portfolio.banking.bankingservice.domain.exception.BalanceFetchException;
import com.portfolio.banking.bankingservice.repository.AccountRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
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

    static WireMockServer wireMockServer = new WireMockServer(8081);

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
        registry.add("banking.account-service-url", () -> "http://localhost:8081");
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
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

        wireMockServer.resetAll();
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/accounts/balance?accountId=account1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account1\",\"balance\":\"1000.00\"}")));
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/accounts/balance?accountId=nonexistent"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Account not found: nonexistent\",\"status\":400}")));
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/accounts/balance?accountId=unavailable"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Service unavailable\",\"status\":500}")));
    }

    @Test
    void initiateTransfer_ValidRequest_PublishesEvent() {
        String fromAccountId = "account1";
        String toAccountId = "account2";
        BigDecimal amount = new BigDecimal("200.00");

        Map<String, Object> response = bankingService.initiateTransfer(fromAccountId, toAccountId, amount);

        assertNotNull(response);
        assertTrue(response.containsKey("transactionId"));
        assertEquals("PENDING", response.get("status"));
    }

    @Test
    void initiateDeposit_ValidRequest_PublishesEvent() {
        String accountId = "account1";
        BigDecimal amount = new BigDecimal("100.00");

        Map<String, Object> response = bankingService.initiateDeposit(accountId, amount);

        assertNotNull(response);
        assertTrue(response.containsKey("transactionId"));
        assertEquals("PENDING", response.get("status"));
    }

    @Test
    void initiateWithdrawal_ValidRequest_PublishesEvent() {
        String accountId = "account1";
        BigDecimal amount = new BigDecimal("100.00");

        Map<String, Object> response = bankingService.initiateWithdrawal(accountId, amount);

        assertNotNull(response);
        assertTrue(response.containsKey("transactionId"));
        assertEquals("PENDING", response.get("status"));
    }

    @Test
    void getBalance_ValidAccount_ReturnsBalance() {
        String accountId = "account1";
        BigDecimal balance = bankingService.getBalance(accountId);

        assertEquals(new BigDecimal("1000.00"), balance);
    }

    @Test
    void getBalance_NonExistentAccount_ThrowsException() {
        String accountId = "nonexistent";
        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance(accountId)
        );
        assertEquals("Failed to fetch balance for account: nonexistent", exception.getMessage());
    }

    @Test
    void getBalance_AccountServiceDown_ThrowsBalanceFetchException() {
        String accountId = "unavailable";
        BalanceFetchException exception = assertThrows(
                BalanceFetchException.class,
                () -> bankingService.getBalance(accountId)
        );
        assertEquals("Failed to fetch balance for account: unavailable", exception.getMessage());
    }
}