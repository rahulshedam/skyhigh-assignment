package com.skyhigh.notification.listener;

import com.skyhigh.notification.event.PaymentEvent;
import com.skyhigh.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentEventListener.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private PaymentEvent paymentCompletedEvent;
    private PaymentEvent paymentFailedEvent;

    @BeforeEach
    void setUp() {
        paymentCompletedEvent = PaymentEvent.builder()
                .paymentReference("PAY123456")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .eventType("PAYMENT_COMPLETED")
                .build();

        paymentFailedEvent = PaymentEvent.builder()
                .paymentReference("PAY123457")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .eventType("PAYMENT_FAILED")
                .build();
    }

    @Test
    void testHandlePaymentEvent_CompletedSuccess() {
        // Given
        doNothing().when(notificationService).sendPaymentConfirmationEmail(any(PaymentEvent.class));

        // When
        paymentEventListener.handlePaymentEvent(paymentCompletedEvent);

        // Then
        verify(notificationService, times(1)).sendPaymentConfirmationEmail(paymentCompletedEvent);
        verify(notificationService, never()).sendPaymentFailureEmail(any());
    }

    @Test
    void testHandlePaymentEvent_FailedSuccess() {
        // Given
        doNothing().when(notificationService).sendPaymentFailureEmail(any(PaymentEvent.class));

        // When
        paymentEventListener.handlePaymentEvent(paymentFailedEvent);

        // Then
        verify(notificationService, times(1)).sendPaymentFailureEmail(paymentFailedEvent);
        verify(notificationService, never()).sendPaymentConfirmationEmail(any());
    }

    @Test
    void testHandlePaymentEvent_UnknownEventType() {
        // Given
        PaymentEvent unknownEvent = PaymentEvent.builder()
                .paymentReference("PAY123458")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .eventType("UNKNOWN_TYPE")
                .build();

        // When
        paymentEventListener.handlePaymentEvent(unknownEvent);

        // Then
        verify(notificationService, never()).sendPaymentConfirmationEmail(any());
        verify(notificationService, never()).sendPaymentFailureEmail(any());
    }

    @Test
    void testHandlePaymentEvent_CompletedThrowsException() {
        // Given
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendPaymentConfirmationEmail(any(PaymentEvent.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                paymentEventListener.handlePaymentEvent(paymentCompletedEvent));
        
        verify(notificationService, times(1)).sendPaymentConfirmationEmail(paymentCompletedEvent);
    }

    @Test
    void testHandlePaymentEvent_FailedThrowsException() {
        // Given
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendPaymentFailureEmail(any(PaymentEvent.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                paymentEventListener.handlePaymentEvent(paymentFailedEvent));
        
        verify(notificationService, times(1)).sendPaymentFailureEmail(paymentFailedEvent);
    }
}
