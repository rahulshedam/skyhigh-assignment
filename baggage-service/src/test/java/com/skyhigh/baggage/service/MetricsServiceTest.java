package com.skyhigh.baggage.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void recordValidation_IncrementsCounter() {
        // Act
        metricsService.recordValidation();

        // Assert
        Counter counter = meterRegistry.find("baggage.validations.total").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordExcessBaggage_IncrementsCounter() {
        // Act
        metricsService.recordExcessBaggage();

        // Assert
        Counter counter = meterRegistry.find("baggage.excess.total").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordValidBaggage_IncrementsCounter() {
        // Act
        metricsService.recordValidBaggage();

        // Assert
        Counter counter = meterRegistry.find("baggage.valid.total").counter();
        assertEquals(1.0, counter.count());
    }
}
