package com.skyhigh.payment.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for tracking payment metrics using Micrometer.
 */
@Slf4j
@Service
public class MetricsService {

    private final Counter totalPaymentsCounter;
    private final Counter successfulPaymentsCounter;
    private final Counter failedPaymentsCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.totalPaymentsCounter = Counter.builder("payment.total")
                .description("Total number of payment attempts")
                .register(meterRegistry);

        this.successfulPaymentsCounter = Counter.builder("payment.success")
                .description("Number of successful payments")
                .register(meterRegistry);

        this.failedPaymentsCounter = Counter.builder("payment.failure")
                .description("Number of failed payments")
                .register(meterRegistry);
    }

    public void incrementTotalPayments() {
        totalPaymentsCounter.increment();
        log.debug("Incremented total payments counter");
    }

    public void incrementSuccessfulPayments() {
        totalPaymentsCounter.increment();
        successfulPaymentsCounter.increment();
        log.debug("Incremented successful payments counter");
    }

    public void incrementFailedPayments() {
        totalPaymentsCounter.increment();
        failedPaymentsCounter.increment();
        log.debug("Incremented failed payments counter");
    }
}
