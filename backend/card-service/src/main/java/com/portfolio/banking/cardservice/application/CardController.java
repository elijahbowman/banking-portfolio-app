//package com.portfolio.banking.cardservice.application;
//
//import com.portfolio.banking.cardservice.domain.entity.Card;
//import com.portfolio.banking.cardservice.domain.service.CardService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.math.BigDecimal;
//
//@RestController
//@RequestMapping("/api/v1/cards")
//@RequiredArgsConstructor
//public class CardController {
//
//    private final CardService cardService;
//
//    @PostMapping("/vcn")
//    public ResponseEntity<Card> issueVCN(
//            @RequestParam String accountId,
//            @RequestParam BigDecimal limit) {
//        return ResponseEntity.ok(cardService.issueVCN(accountId, limit));
//    }
//}