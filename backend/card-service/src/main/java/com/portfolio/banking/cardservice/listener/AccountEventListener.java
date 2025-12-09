package com.portfolio.banking.cardservice.listener;

import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventListener {

    private final CardRepository repository;

    @KafkaListener(topics = "account-created", groupId = "card-service-group")
    public void handleAccountCreated(Map<String, Object> event) {
        String accountId = (String) event.get("accountId");
        String customerName = (String) event.get("customerName");

        if (accountId == null || customerName == null) {
            log.warn("Invalid account-created event: {}", event);
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
        log.info("RealCard created for account {}: {}", accountId, realCard.getCardId());
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
        boolean doubleIt = true;  // 2nd from right
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