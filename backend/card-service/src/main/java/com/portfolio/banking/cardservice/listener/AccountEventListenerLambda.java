package com.portfolio.banking.cardservice.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.repository.CardRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AccountEventListenerLambda {

    private final CardRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObservationRegistry observationRegistry;

    @Bean
    public Consumer<Message<Map<String, Object>>> accountCreated() {
        return message -> {
            Observation.createNotStarted("kafka.batch.process", observationRegistry)
                .observe(() -> {
                    Map<String, Object> payload = message.getPayload();

                    @SuppressWarnings("unchecked")
                    Map<String, List<Map<String, Object>>> recordsMap = (Map<String, List<Map<String, Object>>>) payload.get("records");

                    if (recordsMap == null) return;

                    recordsMap.values().forEach(partitionRecords -> {
                        partitionRecords.forEach(record -> {
                            Observation.createNotStarted("kafka.batch.process", observationRegistry)
                                .observe(() -> {
                                    try {
                                        String base64Value = (String) record.get("value");
                                        if (base64Value == null) {
                                            log.warn("Empty value in record");
                                            return;
                                        }

                                        String json = new String(Base64.getDecoder().decode(base64Value));
                                        Map<String, Object> event = objectMapper.readValue(json, new TypeReference<>() {
                                        });

                                        String eventType = (String) event.get("eventType");
                                        String customerName = (String) event.get("customerName");
                                        String accountId = (String) event.get("accountId");

                                        if (eventType == null || accountId == null || customerName == null) {
                                            log.warn("Invalid event: {}", event);
                                            return;
                                        }

                                        RealCard realCard = RealCard.builder()
                                                .cardId("REAL#" + accountId)
                                                .accountId(accountId)
                                                .cardNumber(generateCardNumber())
                                                .cardHolderName(customerName.toUpperCase())
                                                .expiryDate(generateExpiry())
                                                .status("ACTIVE")
                                                .build();

                                        repository.saveRealCard(realCard);

                                        log.info("RealCard created: {}", realCard.getCardId());

                                    } catch (Exception e) {
                                        log.error("Failed to process record: {}", record, e);
                                    }
                                });
                        });
                    });
                });
        };
    }

    private String generateCardNumber() {
// Simple demo: Visa BIN + random + Luhn
        String bin = "45321234";
        String random = String.format("%07d", System.nanoTime() % 10_000_000);
        String base = bin + random;
        int check = calculateLuhn(base);
        return base + check;
    }
    private int calculateLuhn(String number) {
        int sum = 0;
        boolean doubleIt = true; // 2nd from right
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }
    private String generateExpiry() {
        return YearMonth.now().plusYears(3).format(DateTimeFormatter.ofPattern("MM/yy"));
    }
}