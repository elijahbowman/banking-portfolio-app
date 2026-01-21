//package com.portfolio.banking.cardservice.infrastructure.config;
//
//import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.logs.SdkLoggerProvider;
//import io.opentelemetry.sdk.logs.export.LogRecordExporter;
//import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
//import io.opentelemetry.sdk.metrics.SdkMeterProvider;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
//import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
//import io.opentelemetry.sdk.trace.export.SpanExporter;
//import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
//import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//@Configuration
//public class OTelProcessorConfig {
//
//    @Value("${otel.exporter.otlp.endpoint}")
//    private String otlpEndpoint;
//
//    @Bean
//    @Primary
//    public OpenTelemetry openTelemetry() {
//        // 1. Manually create the Exporters (Matching protocol: http/protobuf)
//        SpanExporter spanExporter = OtlpHttpSpanExporter.builder()
//                .setEndpoint(otlpEndpoint + "/v1/traces")
//                .build();
//
//        LogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
//                .setEndpoint(otlpEndpoint + "/v1/logs")
//                .build();
//
//        // 2. Wrap them in SIMPLE (Synchronous) processors for Lambda
//        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
//                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
//                .build();
//
//        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
//                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
//                .build();
//
//        // 3. Complete the SDK
//        return OpenTelemetrySdk.builder()
//                .setTracerProvider(tracerProvider)
//                .setLoggerProvider(loggerProvider)
//                .setMeterProvider(SdkMeterProvider.builder().build())
//                .buildAndRegisterGlobal();
//    }
//}