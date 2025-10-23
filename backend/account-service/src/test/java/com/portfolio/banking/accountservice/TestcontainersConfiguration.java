package com.portfolio.banking.accountservice;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
				.withDatabaseName("testdb")
				.withUsername("testuser")
				.withPassword("testpass");
	}

//	@Bean
//	public NewTopic myTopic() {
//		return TopicBuilder.name("my-topic")
//				.partitions(1)
//				.replicas(1)
//				.build();
//	}

//	@Bean
//	public Collection<NewTopic> testTopics() {
//		return List.of(
//				TopicBuilder.name("transfer-events")
//						.partitions(1)
//						.replicas(1)
//						.build(),
//				TopicBuilder.name("rollback-events")
//						.partitions(1)
//						.replicas(1)
//						.build()
//		);
//	}
}
