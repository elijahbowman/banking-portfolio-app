package com.portfolio.banking.cardservice;

import org.springframework.boot.SpringApplication;

public class TestCardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CardServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
