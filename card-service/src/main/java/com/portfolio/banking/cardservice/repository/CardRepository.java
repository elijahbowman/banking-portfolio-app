package com.portfolio.banking.cardservice.repository;

import com.portfolio.banking.cardservice.domain.entity.Card;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class CardRepository {

    private final DynamoDbTable<Card> cardTable;

    public CardRepository(DynamoDbEnhancedClient enhancedClient) {
        this.cardTable = enhancedClient.table("Cards", TableSchema.fromBean(Card.class));
    }

    public void save(Card card) {
        cardTable.putItem(card);
    }

    public Card findById(String cardId) {
        return cardTable.getItem(Key.builder().partitionValue(cardId).build());
    }
}