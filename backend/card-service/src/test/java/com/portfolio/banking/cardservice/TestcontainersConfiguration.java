package com.portfolio.banking.cardservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	LocalStackContainer localStackContainer() {
		return new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
				.withServices(LocalStackContainer.Service.DYNAMODB);
	}

	@Bean
	DynamoDbClient dynamoDbClient(LocalStackContainer localStack) {
		return DynamoDbClient.builder()
				.endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("test", "test")
				))
				.build();
	}

	@Bean
	DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
		return DynamoDbEnhancedClient.builder()
				.dynamoDbClient(dynamoDbClient)
				.build();
	}

}
