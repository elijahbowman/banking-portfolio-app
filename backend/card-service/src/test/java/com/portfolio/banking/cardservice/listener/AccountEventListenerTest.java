//package com.portfolio.banking.cardservice.listener;
//
//import com.portfolio.banking.cardservice.domain.entity.RealCard;
//import com.portfolio.banking.cardservice.repository.CardRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.YearMonth;
//import java.time.format.DateTimeFormatter;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoInteractions;
//
//@ExtendWith(MockitoExtension.class)
//class AccountEventListenerTest {
//
//    @Mock private CardRepository repository;
//    @InjectMocks private AccountEventListener listener;
//    @Captor private ArgumentCaptor<RealCard> cardCaptor;
//
//    @Test
//    void shouldCreateRealCardOnValidEvent() {
//        Map<String, Object> event = Map.of(
//                "accountId", "acc123",
//                "customerName", "John Doe"
//        );
//
//        listener.handleAccountCreated(event);
//
//        verify(repository).saveRealCard(cardCaptor.capture());
//        RealCard card = cardCaptor.getValue();
//
//        assertThat(card.getCardId()).isEqualTo("REAL#acc123");
//        assertThat(card.getAccountId()).isEqualTo("acc123");
//        assertThat(card.getCardHolderName()).isEqualTo("JOHN DOE");
//        assertThat(card.getCardNumber()).hasSize(16);
//        assertThat(card.getStatus()).isEqualTo("ACTIVE");
//        assertThat(card.getExpiryDate())
//                .isEqualTo(YearMonth.now().plusYears(3).format(DateTimeFormatter.ofPattern("MM/yy")));
//        assertThat(isValidLuhn(card.getCardNumber())).isTrue();
//    }
//
//    @Test
//    void shouldSkipIfMissingAccountId() {
//        Map<String, Object> event = Map.of("customerName", "John");
//        listener.handleAccountCreated(event);
//        verifyNoInteractions(repository);
//    }
//
//    @Test
//    void shouldSkipIfMissingCustomerName() {
//        Map<String, Object> event = Map.of("accountId", "acc123");
//        listener.handleAccountCreated(event);
//        verifyNoInteractions(repository);
//    }
//
//    private boolean isValidLuhn(String number) {
//        int sum = 0;
//        boolean alternate = false;
//        for (int i = number.length() - 1; i >= 0; i--) {
//            int digit = Character.getNumericValue(number.charAt(i));
//            if (alternate) {
//                digit *= 2;
//                if (digit > 9) digit -= 9;
//            }
//            sum += digit;
//            alternate = !alternate;
//        }
//        return sum % 10 == 0;
//    }
//}