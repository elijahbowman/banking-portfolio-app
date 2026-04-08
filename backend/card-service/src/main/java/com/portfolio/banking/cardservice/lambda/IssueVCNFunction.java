package com.portfolio.banking.cardservice.lambda;

import com.portfolio.banking.cardservice.domain.entity.RealCard;
import com.portfolio.banking.cardservice.domain.entity.VirtualCardToken;
import com.portfolio.banking.cardservice.domain.service.CardService;
//import com.portfolio.banking.cardservice.infrastructure.config.TelemetryFlusher;
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class IssueVCNFunction {

    private final CardService cardService;

    @Autowired
    private MeterRegistry registry;

    public void recordVcnIssuance(String status) {
        // Micrometer will export this to Prometheus as: vcn_issuance_total{status="success"}
        registry.counter("vcn.issuance", "status", status).increment();
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<VirtualCardToken>> issueVCN() {
        return message -> {
            Map<String, Object> payload = message.getPayload();

            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) payload.get("queryStringParameters");

            String realCardId = queryParams.get("realCardId");
            String limitStr = queryParams.get("limit");

            if (realCardId == null || limitStr == null) {
                throw new IllegalArgumentException("realCardId and limit are required");
            }

            BigDecimal limit = new BigDecimal(limitStr);
            VirtualCardToken token = cardService.issueVCN(realCardId, limit);

            recordVcnIssuance("success");

            return MessageBuilder
                    .withPayload(token)
                    .setHeader("Content-Type", "application/json")
                    .build();
        };
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<RealCard>> getRealCard() {
        return message -> {
            Map<String, Object> payload = message.getPayload();
            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) payload.get("queryStringParameters");

            String accountId = queryParams.get("accountId");
            if (accountId == null) {
                throw new IllegalArgumentException("accountId required");
            }

            String realCardId = "REAL#" + accountId;
            RealCard card = cardService.getRealCard(realCardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            return MessageBuilder.withPayload(card).setHeader("Content-Type", "application/json").build();
        };
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<List<VirtualCardToken>>> getVCNs() {
        return message -> {
            Map<String, Object> payload = message.getPayload();
            @SuppressWarnings("unchecked")
            Map<String, String> queryParams = (Map<String, String>) payload.get("queryStringParameters");

            String realCardId = queryParams.get("realCardId");
            if (realCardId == null) {
                throw new IllegalArgumentException("realCardId required");
            }

            List<VirtualCardToken> tokens = cardService.getVCNsForRealCard(realCardId);

            return MessageBuilder.withPayload(tokens).setHeader("Content-Type", "application/json").build();
        };
    }

    @Bean
    public Function<Message<Map<String, Object>>, Message<?>> lambdaRouter() {
        return message -> {
            try {
//                testMetric();
                log.info("lambdaRouter received message={}", message);
                Map<String, Object> payload = message.getPayload();
                @SuppressWarnings("unchecked")
                Map<String, Object> requestContext = (Map<String, Object>) payload.get("requestContext");
                log.info("lambdaRouter requestContext={}", message);

                String httpMethod = (String) requestContext.get("httpMethod");

                log.info("lambdaRouter httpMethod={}", httpMethod);

                if ("OPTIONS".equals(httpMethod)) {
                    return createResponse("", HttpStatus.OK);
                }

                String path = (String) requestContext.get("path");

                if (path.endsWith("/cards/real")) {
                    return getRealCard().apply(message);
                } else if (path.endsWith("/cards/vcns")) {
                    return getVCNs().apply(message);
                } else if (path.endsWith("/cards/vcn")) {
                    return issueVCN().apply(message);
                }

                throw new IllegalArgumentException("Unknown endpoint: " + path);

            } catch (IllegalArgumentException ex) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", ex.getMessage());
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return MessageBuilder.withPayload(response)
                        .setHeader("statusCode", HttpStatus.BAD_REQUEST.value())
                        .setHeader("Content-Type", "application/json").build();
            } catch (IllegalStateException ex) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", ex.getMessage());
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return MessageBuilder.withPayload(response)
                        .setHeader("statusCode", HttpStatus.BAD_REQUEST.value())
                        .setHeader("Content-Type", "application/json").build();
            } catch (Exception ex) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Internal Server Error");
                response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                return MessageBuilder.withPayload(response)
                        .setHeader("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .setHeader("Content-Type", "application/json").build();
            }
        };
    }

    /**
     * Helper to ensure EVERY response contains proper CORS headers.
     */
    private Message<?> createResponse(Object payload, HttpStatus status) {
        return MessageBuilder.withPayload(payload)
                .setHeader("statusCode", status.value())
                .setHeader("Content-Type", "application/json")
                // Essential CORS headers for Lambda Proxy Integration
                .setHeader("Access-Control-Allow-Origin", "*")
                .setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
                .setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization")
                .build();
    }
}