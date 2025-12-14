package com.portfolio.banking.cardservice.lambda;

import com.portfolio.banking.cardservice.TestcontainersConfiguration;
import com.portfolio.banking.cardservice.client.AccountBalanceResponse;
import com.portfolio.banking.cardservice.client.AccountClient;
import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
class IssueVCNFunctionTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private FunctionCatalog catalog;
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;
    @MockitoBean
    private AccountClient accountClient;
    @Autowired
    private CardRepository repository;

    @DynamicPropertySource
    static void registerLocalStackProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.dynamodb.endpoint",
                localstack::getEndpoint);
    }

    @BeforeEach
    void setUp() {
        try {
            enhancedClient.table("Cards", TableSchema.fromBean(RealCard.class)).deleteTable();
            enhancedClient.table("VirtualCardTokens", TableSchema.fromBean(VirtualCardToken.class)).deleteTable();
        } catch (Exception e) {
            // Ignore if table doesn't exist yet
        }

        enhancedClient.table("Cards", TableSchema.fromBean(RealCard.class)).createTable();
        enhancedClient.table("VirtualCardTokens", TableSchema.fromBean(VirtualCardToken.class)).createTable();
    }

    @Test
    void shouldIssueVCNViaLambda_GenericMessage() {
        String accountId = "acc123";
        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId(accountId)
                .cardNumber("4532123456789012")
                .cardHolderName("JOHN DOE")
                .expiryDate("12/28")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        when(accountClient.getBalance(accountId)).thenReturn(new AccountBalanceResponse(BigDecimal.valueOf(1000), accountId));

        Map<String, String> queryParams = Map.of(
                "realCardId", "REAL#acc123",
                "limit", "500"
        );

        // Build headers as Map<String, Object>
        Map<String, Object> payload = Map.of("queryStringParameters", queryParams);

        Message<Map<String, Object>> message = new GenericMessage<>(
                payload,   // payload
                Map.of()   // headers
        );

        Function<Message<Map<String, Object>>, Message<VirtualCardToken>> function =
                catalog.lookup(Function.class, "issueVCN");

        Message<VirtualCardToken> response = function.apply(message);

        VirtualCardToken token = response.getPayload();
        assertThat(token.getVcn()).hasSize(19);
        assertThat(token.getSpendLimit()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(token.getRealCardId()).isEqualTo("REAL#acc123");
    }

    @Test
    void shouldIssueVCNViaLambda_MessageBuilder() {
        String accountId = "acc123";
        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId(accountId)
                .cardNumber("4532123456789012")
                .cardHolderName("JOHN DOE")
                .expiryDate("12/28")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        when(accountClient.getBalance(accountId)).thenReturn(new AccountBalanceResponse(BigDecimal.valueOf(1000), accountId));

        Object queryParams = Map.of(
                "realCardId", "REAL#acc123",
                "limit", "500"
        );

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(Map.of("queryStringParameters", queryParams))
                .build();

        Function<Message<Map<String, Object>>, Message<VirtualCardToken>> function =
                catalog.lookup(Function.class, "issueVCN");

        Message<VirtualCardToken> response = function.apply(message);

        VirtualCardToken token = response.getPayload();
        assertThat(token.getVcn()).hasSize(19);
        assertThat(token.getSpendLimit()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void shouldGetRealCardViaLambda() {
        String accountId = "acc123";
        String realCardId = "REAL#" + accountId;
        RealCard expectedCard = RealCard.builder()
                .cardId(realCardId)
                .accountId(accountId)
                .status("ACTIVE")
                .build();

        repository.saveRealCard(expectedCard);

        Object queryParams = Map.of("accountId", accountId);

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(Map.of("queryStringParameters", queryParams))
                .build();

        Function<Message<Map<String, Object>>, Message<RealCard>> function = catalog.lookup(Function.class, "getRealCard");

        Message<RealCard> response = function.apply(message);

        RealCard card = response.getPayload();
        assertThat(card.getCardId()).isEqualTo(realCardId);
        assertThat(card.getAccountId()).isEqualTo(accountId);
    }

    @Test
    void shouldGetVCNsViaLambda() {
        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId("acc123")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        String realCardId = "REAL#acc123";
        VirtualCardToken token1 = VirtualCardToken.builder()
                .tokenId("VCN#1")
                .realCardId(realCardId)
                .vcn("4532 1234 5678 9012")
                .spendLimit(BigDecimal.valueOf(500))
                .build();
        VirtualCardToken token2 = VirtualCardToken.builder()
                .tokenId("VCN#2")
                .realCardId(realCardId)
                .vcn("4532 9876 5432 1098")
                .spendLimit(BigDecimal.valueOf(300))
                .build();
        repository.saveToken(token1);
        repository.saveToken(token2);

        Object queryParams = Map.of("realCardId", realCardId);

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(Map.of("queryStringParameters", queryParams))
                .build();

        Function<Message<Map<String, Object>>, Message<List<VirtualCardToken>>> function = catalog.lookup(Function.class, "getVCNs");

        Message<List<VirtualCardToken>> response = function.apply(message);

        List<VirtualCardToken> tokens = response.getPayload();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).getTokenId()).isEqualTo("VCN#1");
        assertThat(tokens.get(1).getTokenId()).isEqualTo("VCN#2");
    }
}