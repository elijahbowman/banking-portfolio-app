package com.portfolio.banking.cardservice.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationConfig {
//    @Bean
//    public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
//        ObservationRegistry registry = ObservationRegistry.create();
//        registry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
//        return registry;
//    }

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}