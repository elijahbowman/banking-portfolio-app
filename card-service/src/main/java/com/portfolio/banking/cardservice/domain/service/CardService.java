package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.domain.entity.Card;
import com.portfolio.banking.cardservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Card issueVCN(String accountId, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        Card card = new Card();
        card.setCardId(UUID.randomUUID().toString());
        card.setAccountId(accountId);
        card.setUsageLimit(limit);
        card.setVcn(generateVCN());
        card.setExpiresAt(LocalDateTime.now().plusMonths(1));

        cardRepository.save(card);

        // Publish event to Kafka for Lambda trigger
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "VCN_ISSUED");
        event.put("cardId", card.getCardId());
        event.put("accountId", card.getAccountId());
        event.put("vcn", card.getVcn());
        event.put("usageLimit", card.getUsageLimit().toString());
        event.put("createdAt", card.getCreatedAt().toString());
        event.put("expiresAt", card.getExpiresAt().toString());
        kafkaTemplate.send("vcn-events", card.getCardId(), event);

        return card;
    }

    private String generateVCN() {
        // Simple demo generation; in production, use secure method
        return "VCN-" + UUID.randomUUID().toString().substring(0, 8);
    }
}