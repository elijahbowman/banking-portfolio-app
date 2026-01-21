//package com.portfolio.banking.cardservice.infrastructure.config;
//
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.logs.SdkLoggerProvider;
//import io.opentelemetry.sdk.metrics.SdkMeterProvider;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class OTelConfig {
//
//    @Bean
//    public SdkTracerProvider sdkTracerProvider(@Qualifier("openTelemetry") OpenTelemetry openTelemetry) {
//        return ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider();
//    }
//
//    @Bean
//    public SdkMeterProvider sdkMeterProvider(@Qualifier("openTelemetry") OpenTelemetry openTelemetry) {
//        return ((OpenTelemetrySdk) openTelemetry).getSdkMeterProvider();
//    }
//
//    @Bean
//    public SdkLoggerProvider sdkLoggerProvider(@Qualifier("openTelemetry") OpenTelemetry openTelemetry) {
//        return ((OpenTelemetrySdk) openTelemetry).getSdkLoggerProvider();
//    }
//}