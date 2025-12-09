package com.portfolio.banking.cardservice.domain.service;

import com.portfolio.banking.cardservice.client.AccountClient;
import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceUnitTest {

    @Mock private CardRepository repository;
    @Mock private AccountClient accountClient;
    @InjectMocks private CardService service;

    @Test
    void shouldIssueVCNSuccessfully() {
        String realCardId = "REAL#acc123";
        String accountId = "acc123";

        RealCard realCard = RealCard.builder()
                .cardId(realCardId)
                .status("ACTIVE")
                .accountId(accountId)
                .build();

        when(repository.findRealCardById(realCardId)).thenReturn(Optional.of(realCard));
        when(accountClient.getBalance(accountId)).thenReturn(BigDecimal.valueOf(1000));

        VirtualCardToken token = service.issueVCN(realCardId, BigDecimal.valueOf(500));

        assertThat(token.getVcn()).matches("\\d{4} \\d{4} \\d{4} \\d{4}");
        assertThat(token.getSpendLimit()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(token.getStatus()).isEqualTo("ACTIVE");
        assertThat(token.getRealCardId()).isEqualTo(realCardId);
        verify(repository).saveToken(token);
    }

    @Test
    void shouldRejectIfRealCardNotFound() {
        when(repository.findRealCardById("INVALID")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.issueVCN("INVALID", BigDecimal.valueOf(100)));
    }

    @Test
    void shouldRejectIfInsufficientFunds() {
        String realCardId = "REAL#acc123";
        String accountId = "acc123";

        RealCard realCard = RealCard.builder()
                .cardId(realCardId)
                .status("ACTIVE")
                .accountId(accountId)
                .build();

        when(repository.findRealCardById(realCardId)).thenReturn(Optional.of(realCard));
        when(accountClient.getBalance(accountId)).thenReturn(BigDecimal.valueOf(400));

        assertThrows(IllegalStateException.class,
                () -> service.issueVCN(realCardId, BigDecimal.valueOf(500)));
    }

    @Test
    void shouldGetRealCardSuccessfully() {
        String realCardId = "REAL#acc123";
        RealCard expected = RealCard.builder()
                .cardId(realCardId)
                .accountId("acc123")
                .status("ACTIVE")
                .build();

        when(repository.findRealCardById(realCardId)).thenReturn(Optional.of(expected));

        Optional<RealCard> result = service.getRealCard(realCardId);

        assertThat(result).isPresent();
        assertThat(result.get().getCardId()).isEqualTo(realCardId);
    }

    @Test
    void shouldReturnEmptyIfRealCardNotFound() {
        when(repository.findRealCardById("INVALID")).thenReturn(Optional.empty());  // ← STUB

        Optional<RealCard> result = service.getRealCard("INVALID");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetVCNsForRealCardSuccessfully() {
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

        when(repository.findTokensByRealCard(realCardId)).thenReturn(Stream.of(token1, token2));

        List<VirtualCardToken> result = service.getVCNsForRealCard(realCardId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTokenId()).isEqualTo("VCN#1");
    }

    @Test
    void shouldReturnEmptyListIfNoVCNs() {
        when(repository.findTokensByRealCard("REAL#acc123")).thenReturn(Stream.empty());

        List<VirtualCardToken> result = service.getVCNsForRealCard("REAL#acc123");

        assertThat(result).isEmpty();
    }
}