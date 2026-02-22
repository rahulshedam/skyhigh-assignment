package com.skyhigh.payment.service;

import com.skyhigh.payment.service.PaymentSimulationService.PaymentSimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSimulationServiceTest {

    private PaymentSimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService = new PaymentSimulationService();
        ReflectionTestUtils.setField(simulationService, "processingDelayMs", 0);
        ReflectionTestUtils.setField(simulationService, "successRate", 1.0); // deterministic success when flag not set
    }

    @Test
    void simulatePayment_Success() {
        PaymentSimulationResult result = simulationService.simulatePayment(false);

        assertTrue(result.isSuccess());
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN-"));
        assertNull(result.getFailureReason());
    }

    @Test
    void simulatePayment_ExplicitFailureFlag_AlwaysFails() {
        PaymentSimulationResult result = simulationService.simulatePayment(true);

        assertFalse(result.isSuccess());
        assertNull(result.getTransactionId());
        assertEquals("INSUFFICIENT_FUNDS", result.getFailureReason());
    }

    @Test
    void simulatePayment_NullFailureFlag_ProbabilisticWithSuccessRate() {
        PaymentSimulationResult result = simulationService.simulatePayment(null);
        // successRate=1.0 in setUp => always success when flag is null
        assertTrue(result.isSuccess());
        assertNotNull(result.getTransactionId());
        assertNull(result.getFailureReason());
    }

    @Test
    void simulatePayment_ProbabilisticFailureWhenSuccessRateZero() {
        ReflectionTestUtils.setField(simulationService, "successRate", 0.0);
        PaymentSimulationResult result = simulationService.simulatePayment(null);

        assertFalse(result.isSuccess());
        assertNull(result.getTransactionId());
        assertNotNull(result.getFailureReason());
    }
}
