package com.skyhigh.baggage.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for recording baggage metrics.
 */
@Slf4j
@Service
public class MetricsService {

    private final Counter totalValidationsCounter;
    private final Counter excessBaggageCounter;
    private final Counter validBaggageCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.totalValidationsCounter = Counter.builder("baggage.validations.total")
                .description("Total number of baggage validations")
                .register(meterRegistry);

        this.excessBaggageCounter = Counter.builder("baggage.excess.total")
                .description("Total number of excess baggage cases")
                .register(meterRegistry);

        this.validBaggageCounter = Counter.builder("baggage.valid.total")
                .description("Total number of valid baggage (within limit)")
                .register(meterRegistry);
    }

    public void recordValidation() {
        totalValidationsCounter.increment();
        log.debug("Recorded baggage validation");
    }

    public void recordExcessBaggage() {
        excessBaggageCounter.increment();
        log.debug("Recorded excess baggage");
    }

    public void recordValidBaggage() {
        validBaggageCounter.increment();
        log.debug("Recorded valid baggage");
    }
}
