package com.skyhigh.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for simulating payment gateway behavior.
 * Supports probabilistic success/failure (configurable success rate, default 70%)
 * and an explicit simulateFailure flag for deterministic test scenarios.
 */
@Slf4j
@Service
public class PaymentSimulationService {

    private static final String[] FAILURE_REASONS = {
            "PAYMENT_DECLINED",
            "INSUFFICIENT_FUNDS",
            "CARD_EXPIRED",
            "FRAUD_DETECTION",
            "CARD_LIMIT_EXCEEDED"
    };

    @Value("${payment.simulation.processing-delay-ms:500}")
    private int processingDelayMs;

    @Value("${payment.simulation.success-rate:0.70}")
    private double successRate;

    /**
     * Simulate payment processing with probabilistic behavior.
     * <ul>
     *   <li>If simulateFailure is explicitly true: always fails (for testing).</li>
     *   <li>Otherwise: success with probability {@code successRate} (e.g. 0.70 = 70% success, 30% failure).</li>
     * </ul>
     *
     * @param simulateFailure If true, payment always fails; if false/null, outcome is probabilistic
     * @return PaymentSimulationResult with success status and transaction ID or failure reason
     */
    public PaymentSimulationResult simulatePayment(Boolean simulateFailure) {
        log.debug("Simulating payment with simulateFailure={}, successRate={}", simulateFailure, successRate);

        // Simulate processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Payment simulation interrupted", e);
        }

        // Explicit flag: always fail (for deterministic tests)
        if (Boolean.TRUE.equals(simulateFailure)) {
            log.info("Simulating payment failure (explicit flag)");
            return PaymentSimulationResult.failure("INSUFFICIENT_FUNDS");
        }

        // Probabilistic outcome based on configured success rate
        double random = Math.random();
        if (random < successRate) {
            String transactionId = generateTransactionId();
            log.info("Simulating payment success with transaction ID: {}", transactionId);
            return PaymentSimulationResult.success(transactionId);
        } else {
            String reason = FAILURE_REASONS[(int) (Math.random() * FAILURE_REASONS.length)];
            log.info("Simulating payment failure (probabilistic): {}", reason);
            return PaymentSimulationResult.failure(reason);
        }
    }

    /**
     * Generate a mock transaction ID
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Result of payment simulation
     */
    public static class PaymentSimulationResult {
        private final boolean success;
        private final String transactionId;
        private final String failureReason;

        private PaymentSimulationResult(boolean success, String transactionId, String failureReason) {
            this.success = success;
            this.transactionId = transactionId;
            this.failureReason = failureReason;
        }

        public static PaymentSimulationResult success(String transactionId) {
            return new PaymentSimulationResult(true, transactionId, null);
        }

        public static PaymentSimulationResult failure(String failureReason) {
            return new PaymentSimulationResult(false, null, failureReason);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }
}
