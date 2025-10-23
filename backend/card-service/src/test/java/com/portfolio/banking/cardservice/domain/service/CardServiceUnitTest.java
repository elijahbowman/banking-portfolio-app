package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.domain.entity.Card;
import com.portfolio.banking.cardservice.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceUnitTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CardService cardService;

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card();
        card.setCardId("card1");
        card.setAccountId("account1");
        card.setUsageLimit(new BigDecimal("500.00"));
        card.setVcn("VCN-12345678");
        card.setCreatedAt(LocalDateTime.now());
        card.setExpiresAt(LocalDateTime.now().plusMonths(1));
    }

    @Test
    void issueVCN_ValidInput_ReturnsCard() {
        // Arrange
        String accountId = "account1";
        BigDecimal limit = new BigDecimal("500.00");

        doNothing().when(cardRepository).save(any(Card.class));
        when(kafkaTemplate.send(eq("vcn-events"), any(String.class), any(Map.class)))
                .thenReturn(null); // Mock KafkaTemplate send method

        // Act
        Card result = cardService.issueVCN(accountId, limit);

        // Assert
        assertNotNull(result);
        assertEquals(accountId, result.getAccountId());
        assertEquals(limit, result.getUsageLimit());
        assertNotNull(result.getCardId());
        assertNotNull(result.getVcn());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getVcn().startsWith("VCN-"));
        assertEquals(result.getCreatedAt().plusMonths(1).getMonth(), result.getExpiresAt().getMonth());
        verify(cardRepository, times(1)).save(any(Card.class));
        verify(kafkaTemplate, times(1)).send(eq("vcn-events"), any(String.class), any(Map.class));
    }

    @Test
    void issueVCN_ZeroLimit_ThrowsIllegalArgumentException() {
        // Arrange
        String accountId = "account1";
        BigDecimal limit = BigDecimal.ZERO;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                cardService.issueVCN(accountId, limit));
        assertEquals("Limit must be positive", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void issueVCN_NegativeLimit_ThrowsIllegalArgumentException() {
        // Arrange
        String accountId = "account1";
        BigDecimal limit = new BigDecimal("-100.00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                cardService.issueVCN(accountId, limit));
        assertEquals("Limit must be positive", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}