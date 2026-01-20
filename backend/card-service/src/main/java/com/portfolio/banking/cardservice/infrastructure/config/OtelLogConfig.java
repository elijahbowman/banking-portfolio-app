package com.portfolio.banking.cardservice.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class OtelLogConfig {

    private final ObjectProvider<OpenTelemetry> openTelemetryProvider;

    public OtelLogConfig(ObjectProvider<OpenTelemetry> openTelemetryProvider) {
        this.openTelemetryProvider = openTelemetryProvider;
    }

    @PostConstruct
    public void init() {
        // Get the primary OpenTelemetry bean and install it into the Logback appender
        openTelemetryProvider.ifAvailable(OpenTelemetryAppender::install);
    }
}