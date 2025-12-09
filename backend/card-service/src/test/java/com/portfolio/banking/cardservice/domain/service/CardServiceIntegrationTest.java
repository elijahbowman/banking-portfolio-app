package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.TestcontainersConfiguration;
import com.portfolio.banking.cardservice.client.AccountClient;
import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
class CardServiceIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.DYNAMODB);

    @MockitoBean
    private AccountClient accountClient;
    @Autowired
    private CardService service;
    @Autowired
    private CardRepository repository;
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

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
    void endToEndVCNIssuance() {
        String accountId = "acc123";
        when(accountClient.getBalance(accountId)).thenReturn(BigDecimal.valueOf(1000));

        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId(accountId)
                .cardNumber("4532123456789012")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        VirtualCardToken token = service.issueVCN("REAL#acc123", BigDecimal.valueOf(750));

        assertThat(token.getVcn()).hasSize(19);
        assertThat(token.getRealCardId()).isEqualTo("REAL#acc123");
        assertThat(token.getSpendLimit()).isEqualByComparingTo(BigDecimal.valueOf(750));
        assertThat(token.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldGetRealCardSuccessfully() {
        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId("acc123")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        Optional<RealCard> retrieved = service.getRealCard("REAL#acc123");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCardId()).isEqualTo("REAL#acc123");
    }

    @Test
    void shouldReturnEmptyIfRealCardNotFound() {
        Optional<RealCard> retrieved = service.getRealCard("INVALID");

        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldGetVCNsForRealCardSuccessfully() {
        RealCard realCard = RealCard.builder()
                .cardId("REAL#acc123")
                .accountId("acc123")
                .status("ACTIVE")
                .build();
        repository.saveRealCard(realCard);

        VirtualCardToken token1 = VirtualCardToken.builder()
                .tokenId("VCN#1")
                .realCardId("REAL#acc123")
                .vcn("4532 1234 5678 9012")
                .spendLimit(BigDecimal.valueOf(500))
                .build();
        repository.saveToken(token1);

        VirtualCardToken token2 = VirtualCardToken.builder()
                .tokenId("VCN#2")
                .realCardId("REAL#acc123")
                .vcn("4532 9876 5432 1098")
                .spendLimit(BigDecimal.valueOf(300))
                .build();
        repository.saveToken(token2);

        List<VirtualCardToken> tokens = service.getVCNsForRealCard("REAL#acc123");

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).getTokenId()).isEqualTo("VCN#1");
        assertThat(tokens.get(1).getTokenId()).isEqualTo("VCN#2");
    }

    @Test
    void shouldReturnEmptyListIfNoVCNs() {
        List<VirtualCardToken> tokens = service.getVCNsForRealCard("REAL#nonexistent");

        assertThat(tokens).isEmpty();
    }
}