package com.portfolio.banking.cardservice.repository;

import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class CardRepository {

    private final DynamoDbTable<RealCard> realCardTable;
    private final DynamoDbTable<VirtualCardToken> tokenTable;
    private final DynamoDbIndex<VirtualCardToken> realCardIdIndex;

    public CardRepository(DynamoDbEnhancedClient enhancedClient) {

        this.realCardTable =
                enhancedClient.table("Cards", TableSchema.fromBean(RealCard.class));

        this.tokenTable =
                enhancedClient.table("VirtualCardTokens", TableSchema.fromBean(VirtualCardToken.class));

        this.realCardIdIndex =
                enhancedClient.table("VirtualCardTokens", TableSchema.fromBean(VirtualCardToken.class))
                        .index("RealCardIdIndex");
    }

    @Observed(name = "real.card.db.call", contextualName = "dynamo-put-item")
    public void saveRealCard(RealCard card) {
        realCardTable.putItem(card);
    }

    @Observed(name = "real.card.db.call", contextualName = "dynamo-get-item")
    public Optional<RealCard> findRealCardById(String cardId) {
        return Optional.ofNullable(
                realCardTable.getItem(Key.builder().partitionValue(cardId).build())
        );
    }

    @Observed(name = "real.card.db.call", contextualName = "dynamo-get-item")
    public Optional<RealCard> findRealCardByAccountId(String accountId) {
        return Optional.ofNullable(
                realCardTable.getItem(Key.builder().partitionValue("REAL#" + accountId).build())
        );
    }

    @Observed(name = "virtual.card.db.call", contextualName = "dynamo-put-item")
    public void saveToken(VirtualCardToken token) {
        tokenTable.putItem(token);
    }

    @Observed(name = "virtual.card.db.call", contextualName = "dynamo-get-item")
    public Optional<VirtualCardToken> findTokenById(String tokenId) {
        return Optional.ofNullable(
                tokenTable.getItem(Key.builder().partitionValue(tokenId).build())
        );
    }

    @Observed(name = "real.card.db.call", contextualName = "dynamo-get-items-by-query")
    public Stream<VirtualCardToken> findTokensByRealCard(String realCardId) {
        return realCardIdIndex.query(r -> r
                        .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(realCardId)))
                ).stream()
                .flatMap(page -> page.items().stream());
    }
}