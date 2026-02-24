//package com.portfolio.banking.cardservice.infrastructure.config;
//
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//@Component
//@Slf4j
//public class TelemetryFlusher {
//
//    private final List<OpenTelemetry> otelInstances;
//
//    public TelemetryFlusher(List<OpenTelemetry> otelInstances,
//                            @Qualifier("openTelemetry") OpenTelemetry qualifiedInstance) {
//        this.otelInstances = otelInstances;
//
//        // Cold start diagnostics
//        log.info("DIAGNOSTIC: Found {} OpenTelemetry bean(s) in context.", otelInstances.size());
//
//        log.info("The @Qualifier chose: {}", qualifiedInstance.toString());
//
//        // Check which one in the list matches the qualified one
//        for (int i = 0; i < otelInstances.size(); i++) {
//            if (otelInstances.get(i) == qualifiedInstance) {
//                log.info("The @Qualifier matched Bean #{} in the list.", i);
//            }
//        }
//
//        for (int i = 0; i < otelInstances.size(); i++) {
//            OpenTelemetry instance = otelInstances.get(i);
//            log.info("Bean #{} - Type: {} - Implementation: {}",
//                    i,
//                    instance.getClass().getSimpleName(),
//                    instance.toString());
//        }
//    }
//
//    public void flush() {
//        for (OpenTelemetry otel : otelInstances) {
//            if (otel instanceof OpenTelemetrySdk sdk) {
//                // Join with timeout to ensure Lambda stays awake
//                sdk.getSdkTracerProvider().forceFlush().join(5, TimeUnit.SECONDS);
//                sdk.getSdkMeterProvider().forceFlush().join(5, TimeUnit.SECONDS);
//                sdk.getSdkLoggerProvider().forceFlush().join(5, TimeUnit.SECONDS);
//            }
//        }
//    }
//}