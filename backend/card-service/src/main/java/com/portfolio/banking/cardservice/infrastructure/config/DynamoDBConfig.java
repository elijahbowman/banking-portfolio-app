package com.portfolio.banking.cardservice.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Profile("lambda")
public class DynamoDBConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(ObservationRegistry observationRegistry) {
        return DynamoDbClient.builder()
//                .overrideConfiguration(builder -> builder
//                        // This 'hook' allows Micrometer to see the DB calls
//                        .addExecutionInterceptor(new software.amazon.awssdk.core.interceptor.ExecutionInterceptor() {
//                            // Just having a custom interceptor often triggers the Agent's or Spring's internal bridge to finally 'wake up'.
//                        })
//                )
            .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}