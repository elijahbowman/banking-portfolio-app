package com.portfolio.banking.bankingservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BankingServiceApplication {
	private static final Logger logger = LoggerFactory.getLogger(BankingServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BankingServiceApplication.class, args);
	}

//	@Bean
//	ApplicationRunner runner(@Value("${my.value}") String dbPassword) {
//		return args -> logger.info("Retrieved DB password: {}", dbPassword);
//	}
//@Bean
//ApplicationRunner runner(
//		@Value("${spring.profiles.active:default}") String activeProfile,
//		@Value("${spring.datasource.password:NOT_SET}") String dbPassword,
//		@Value("${my.value:NOT_SET}") String myValue,
//		@Value("${my.value2:NOT_SET}") String myValue2,
//		@Value("${spring.kafka.bootstrap-servers:NOT_SET}") String kafkaBroker
//) {
//	return args -> {
//		logger.info("Active profile: {}", activeProfile);
//		logger.info("Retrieved DB password: {}", dbPassword);
//		logger.info("Retrieved my.value: {}", myValue);
//		logger.info("Retrieved my.value2: {}", myValue2);
//		logger.info("Retrieved spring.kafka.bootstrap-servers: {}", kafkaBroker);
//	};
//}
}
