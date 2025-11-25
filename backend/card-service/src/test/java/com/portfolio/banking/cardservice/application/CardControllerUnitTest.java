//package com.portfolio.banking.cardservice.application;
//
//import com.portfolio.banking.cardservice.domain.entity.Card;
//import com.portfolio.banking.cardservice.domain.service.CardService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(controllers = {CardController.class})
//class CardControllerUnitTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private CardService cardService;
//
//    @Test
//    void issueVCN_ValidInput_ReturnsOkWithCard() throws Exception {
//        // Arrange
//        String accountId = "account1";
//        BigDecimal limit = new BigDecimal("500.00");
//
//        Card card = new Card();
//        card.setCardId("card1");
//        card.setAccountId(accountId);
//        card.setUsageLimit(limit);
//        card.setVcn("VCN-12345678");
//        card.setCreatedAt(LocalDateTime.now());
//        card.setExpiresAt(LocalDateTime.now().plusMonths(1));
//
//        when(cardService.issueVCN(accountId, limit)).thenReturn(card);
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/cards/vcn")
//                        .param("accountId", accountId)
//                        .param("limit", limit.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.cardId").value("card1"))
//                .andExpect(jsonPath("$.accountId").value(accountId))
//                .andExpect(jsonPath("$.usageLimit").value(500.00))
//                .andExpect(jsonPath("$.vcn").value("VCN-12345678"));
//    }
//
//    @Test
//    void issueVCN_InvalidLimit_ThrowsBadRequest() throws Exception {
//        // Arrange
//        String accountId = "account1";
//        String invalidLimit = "invalid";
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/cards/vcn")
//                        .param("accountId", accountId)
//                        .param("limit", invalidLimit)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void issueVCN_ServiceThrowsIllegalArgumentException_ReturnsBadRequest() throws Exception {
//        // Arrange
//        String accountId = "account1";
//        BigDecimal limit = BigDecimal.ZERO;
//
//        when(cardService.issueVCN(anyString(), any(BigDecimal.class)))
//                .thenThrow(new IllegalArgumentException("Limit must be positive"));
//
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/cards/vcn")
//                        .param("accountId", accountId)
//                        .param("limit", limit.toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").value("Limit must be positive"))
//                .andExpect(jsonPath("$.status").value(400));
//    }
//
//    @Test
//    void issueVCN_MissingParameters_ReturnsBadRequest() throws Exception {
//        // Act & Assert
//        mockMvc.perform(post("/api/v1/cards/vcn")
//                        .param("accountId", "account1")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest());
//    }
//}