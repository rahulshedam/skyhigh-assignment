package com.skyhigh.payment.service;

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
    void incrementTotalPayments() {
        metricsService.incrementTotalPayments();

        Counter counter = meterRegistry.find("payment.total").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    void incrementSuccessfulPayments() {
        metricsService.incrementSuccessfulPayments();

        Counter totalCounter = meterRegistry.find("payment.total").counter();
        Counter successCounter = meterRegistry.find("payment.success").counter();

        assertEquals(1.0, totalCounter.count());
        assertEquals(1.0, successCounter.count());
    }

    @Test
    void incrementFailedPayments() {
        metricsService.incrementFailedPayments();

        Counter totalCounter = meterRegistry.find("payment.total").counter();
        Counter failureCounter = meterRegistry.find("payment.failure").counter();

        assertEquals(1.0, totalCounter.count());
        assertEquals(1.0, failureCounter.count());
    }
}
