package com.portfolio.banking.bankingservice.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Environment env;

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    public WebConfig(Environment env) {
        this.env = env;
    }

    private String getFrontendCname() {
        try (ElasticBeanstalkClient client = ElasticBeanstalkClient.builder()
                .region(Region.US_EAST_1)
                .build()) {
            DescribeEnvironmentsRequest request = DescribeEnvironmentsRequest.builder()
                    .applicationName("banking-portfolio")
                    .environmentNames("frontend-env")
                    .build();
            DescribeEnvironmentsResponse response = client.describeEnvironments(request);
            String cname = response.environments().get(0).cname();
            String frontendUrl = "http://" + cname;
            logger.info("Fetched frontend CNAME: {}", frontendUrl);
            return frontendUrl;
        } catch (Exception e) {
            logger.error("Failed to fetch frontend CNAME", e);
            throw new RuntimeException("Failed to fetch frontend CNAME", e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = env.getProperty(
                "banking.cors.allowed-origins",
                String[].class
        );
        String[] finalOrigins; // Final after adding Frontend CNAME
        boolean isAwsProfile = Arrays.stream(env.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("docker")); // Check for 'docker' profile

        if (isAwsProfile) {
            String frontendUrl = getFrontendCname();
            // Write to application.properties
            try (FileWriter writer = new FileWriter("/app/application.properties", true)) {
                writer.write("frontend.url=" + frontendUrl + "\n");
            } catch (IOException e) {
                logger.error("Failed to write frontend.url to properties", e);
                throw new RuntimeException("Failed to write frontend.url to properties", e);
            }

            // Combine with predefined origins
            if (allowedOrigins.length > 0) {
                finalOrigins = new String[allowedOrigins.length + 1];
                System.arraycopy(allowedOrigins, 0, finalOrigins, 0, allowedOrigins.length);
                finalOrigins[allowedOrigins.length] = frontendUrl;
            } else {
                finalOrigins = new String[]{frontendUrl};
            }
        } else {
            // Non-docker profile
            if (allowedOrigins.length > 0) {
                finalOrigins = allowedOrigins;
            } else {
                logger.warn("No CORS origins defined for non-AWS profile; defaulting to localhost");
                finalOrigins = new String[]{"http://localhost:3000"}; // Fallback for local dev
            }
        }

        logger.info("allowed CORS origins: {}", Arrays.toString(finalOrigins));
        registry.addMapping("/api/v1/banking/**")
                .allowedOrigins(finalOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}