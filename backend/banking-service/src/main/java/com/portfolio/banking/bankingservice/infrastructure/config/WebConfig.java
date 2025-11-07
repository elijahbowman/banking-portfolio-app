package com.portfolio.banking.bankingservice.infrastructure.config;

import com.portfolio.banking.bankingservice.BankingServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Environment env;

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    public WebConfig(Environment env) {
        this.env = env;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = env.getProperty(
                "banking.cors.allowed-origins",
                String[].class
        );

        logger.info("allowedOrigins: {}", Arrays.toString(allowedOrigins));
        registry.addMapping("/api/v1/banking/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}