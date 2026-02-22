package com.skyhigh.payment.service;

import com.skyhigh.common.dto.ApiResponse;
import com.skyhigh.payment.dto.PaymentData;
import com.skyhigh.payment.dto.PaymentRequest;
import com.skyhigh.payment.event.EventPublisher;
import com.skyhigh.payment.exception.PaymentNotFoundException;
import com.skyhigh.payment.model.Payment;
import com.skyhigh.payment.model.PaymentStatus;
import com.skyhigh.payment.model.PaymentType;
import com.skyhigh.payment.repository.PaymentRepository;
import com.skyhigh.payment.service.PaymentSimulationService.PaymentSimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentSimulationService simulationService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = PaymentRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentType(PaymentType.SEAT_UPGRADE)
                .build();

        payment = Payment.builder()
                .id(1L)
                .paymentReference("PAY-REF-123")
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentType(PaymentType.SEAT_UPGRADE)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void processPayment_Success() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(simulationService.simulatePayment(any())).thenReturn(PaymentSimulationResult.success("TXN-123"));

        ApiResponse<PaymentData> response = paymentService.processPayment(paymentRequest);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("TXN-123", payment.getTransactionId());

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(eventPublisher).publishPaymentCompletedEvent(any());
        verify(metricsService).incrementSuccessfulPayments();
    }

    @Test
    void processPayment_Failure() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(simulationService.simulatePayment(true)).thenReturn(PaymentSimulationResult.failure("INSUFFICIENT_FUNDS"));

        paymentRequest.setSimulateFailure(true);
        ApiResponse<PaymentData> response = paymentService.processPayment(paymentRequest);

        assertFalse(response.isSuccess());
        assertEquals("Payment failed: INSUFFICIENT_FUNDS", response.getMessage());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", payment.getFailureReason());

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(eventPublisher).publishPaymentFailedEvent(any());
        verify(metricsService).incrementFailedPayments();
    }

    @Test
    void getPaymentByReference_Success() {
        when(paymentRepository.findByPaymentReference("PAY-REF-123")).thenReturn(Optional.of(payment));

        ApiResponse<PaymentData> response = paymentService.getPaymentByReference("PAY-REF-123");

        assertTrue(response.isSuccess());
        assertEquals("PAY-REF-123", response.getData().getPaymentReference());
    }

    @Test
    void getPaymentByReference_NotFound() {
        when(paymentRepository.findByPaymentReference(anyString())).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByReference("NON-EXISTENT"));
    }

    @Test
    void getPaymentsByPassengerId() {
        when(paymentRepository.findByPassengerId("PASS123")).thenReturn(Arrays.asList(payment));

        ApiResponse<List<PaymentData>> response = paymentService.getPaymentsByPassengerId("PASS123");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("PASS123", payment.getPassengerId());
    }

    @Test
    void getPaymentsByBookingReference() {
        when(paymentRepository.findByBookingReference("BOOK456")).thenReturn(Arrays.asList(payment));

        ApiResponse<List<PaymentData>> response = paymentService.getPaymentsByBookingReference("BOOK456");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("BOOK456", payment.getBookingReference());
    }
}
