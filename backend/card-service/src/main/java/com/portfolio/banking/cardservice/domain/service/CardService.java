package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.client.AccountClient;
import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final AccountClient accountClient;
    private final CardRepository repository;
    private static final SecureRandom random = new SecureRandom();

    public VirtualCardToken issueVCN(String realCardId, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        RealCard realCard = repository.findRealCardById(realCardId)
                .orElseThrow(() -> new IllegalArgumentException("Real card not found: " + realCardId));

        if (!"ACTIVE".equals(realCard.getStatus())) {
            throw new IllegalStateException("Card is not active");
        }

        BigDecimal availableBalance = accountClient.getBalance(realCard.getAccountId()).balance();

        if (limit.compareTo(availableBalance) > 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        String vcn = generateLuhnValidVCN();
        VirtualCardToken token = VirtualCardToken.builder()
                .tokenId("VCN#" + UUID.randomUUID())
                .realCardId(realCardId)
                .vcn(vcn)
                .spendLimit(limit)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .status("ACTIVE")
                .build();

        repository.saveToken(token);
        return token;
    }

    private String generateLuhnValidVCN() {
        String bin = "453212"; // Configurable in real app
        StringBuilder number = new StringBuilder(bin);
        for (int i = 0; i < 9; i++) {
            number.append(random.nextInt(10));
        }
        number.append(calculateLuhnCheckDigit(number.toString()));
        return formatCardNumber(number.toString());
    }

    private int calculateLuhnCheckDigit(String number) {
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

    private String formatCardNumber(String number) {
        return number.replaceAll(".{4}", "$0 ").trim();
    }

    public Optional<RealCard> getRealCard(String realCardId) {
        return repository.findRealCardById(realCardId);
    }
    public Optional<RealCard> getRealCardByAccountID(String accountId) {
        return repository.findRealCardByAccountId(accountId);
    }

    public List<VirtualCardToken> getVCNsForRealCard(String realCardId) {
        return repository.findTokensByRealCard(realCardId).toList();
    }
}