package com.skyhigh.notification.service;

import com.skyhigh.notification.event.*;
import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MockEmailService mockEmailService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private NotificationService notificationService;

    private SeatConfirmedEvent seatConfirmedEvent;
    private SeatHeldEvent seatHeldEvent;
    private PaymentEvent paymentCompletedEvent;
    private PaymentEvent paymentFailedEvent;
    private WaitlistAssignedEvent waitlistAssignedEvent;

    @BeforeEach
    void setUp() {
        // Setup test data
        seatConfirmedEvent = SeatConfirmedEvent.builder()
                .seatId(1L)
                .seatNumber("12A")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .confirmedAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        seatHeldEvent = SeatHeldEvent.builder()
                .seatId(1L)
                .seatNumber("12A")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .heldAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .timestamp(LocalDateTime.now())
                .build();

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

        waitlistAssignedEvent = WaitlistAssignedEvent.builder()
                .waitlistId(1L)
                .seatId(2L)
                .seatNumber("14B")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .assignedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testSendSeatConfirmationEmail_Success() {
        // Given
        String htmlContent = "<html>Confirmation Email</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatConfirmationEmail(seatConfirmedEvent);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/seat-confirmation"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.SENT, savedNotification.getStatus());
        assertEquals("SKY123456", savedNotification.getBookingReference());
        assertEquals("SEAT_CONFIRMED", savedNotification.getEventType());
    }

    @Test
    void testSendSeatHoldEmail_Success() {
        // Given
        String htmlContent = "<html>Seat Hold Email</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatHoldEmail(seatHeldEvent);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/seat-hold"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendPaymentConfirmationEmail_Success() {
        // Given
        String htmlContent = "<html>Payment Confirmation</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendPaymentConfirmationEmail(paymentCompletedEvent);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/payment-confirmation"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("PAYMENT_COMPLETED", savedNotification.getEventType());
    }

    @Test
    void testSendPaymentFailureEmail_Success() {
        // Given
        String htmlContent = "<html>Payment Failed</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendPaymentFailureEmail(paymentFailedEvent);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/payment-failure"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("PAYMENT_FAILED", savedNotification.getEventType());
    }

    @Test
    void testSendWaitlistNotificationEmail_Success() {
        // Given
        String htmlContent = "<html>Waitlist Assigned</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendWaitlistNotificationEmail(waitlistAssignedEvent);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/waitlist-assigned"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendSeatConfirmationEmail_TemplateServiceThrowsException() {
        // Given
        when(templateService.renderTemplate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Template error"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatConfirmationEmail(seatConfirmedEvent);

        // Then - Should save failed notification
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
        assertNotNull(savedNotification.getErrorMessage());
    }

    @Test
    void testSendPaymentConfirmationEmail_EmailServiceThrowsException() {
        // Given
        String htmlContent = "<html>Payment Confirmation</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        doThrow(new RuntimeException("Email send error")).when(mockEmailService)
                .sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendPaymentConfirmationEmail(paymentCompletedEvent);

        // Then - Should save failed notification
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
    }

    @Test
    void testSendSeatHoldEmail_TemplateServiceThrowsException() {
        // Given
        when(templateService.renderTemplate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Template rendering failed"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatHoldEmail(seatHeldEvent);

        // Then - Should save failed notification
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
        assertEquals("SEAT_HELD", savedNotification.getEventType());
    }

    @Test
    void testSendPaymentFailureEmail_TemplateServiceThrowsException() {
        // Given
        when(templateService.renderTemplate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Template rendering failed"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendPaymentFailureEmail(paymentFailedEvent);

        // Then - Should save failed notification
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
        assertEquals("PAYMENT_FAILED", savedNotification.getEventType());
    }

    @Test
    void testSendWaitlistNotificationEmail_TemplateServiceThrowsException() {
        // Given
        when(templateService.renderTemplate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Template rendering failed"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendWaitlistNotificationEmail(waitlistAssignedEvent);

        // Then - Should save failed notification
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
        assertEquals("WAITLIST_ASSIGNED", savedNotification.getEventType());
    }

    @Test
    void testSendSeatConfirmationEmail_PassengerIdWithEmail_UsesEmailDirectly() {
        // Given - passengerId is already an email
        SeatConfirmedEvent eventWithEmail = SeatConfirmedEvent.builder()
                .seatId(1L)
                .seatNumber("12A")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("passenger@example.com")
                .bookingReference("SKY123456")
                .confirmedAt(null)
                .timestamp(LocalDateTime.now())
                .build();

        String htmlContent = "<html>Confirmation Email</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatConfirmationEmail(eventWithEmail);

        // Then - Email should be sent directly using the passenger ID as email
        verify(mockEmailService, times(1)).sendEmail(eq("passenger@example.com"), anyString(), anyString());
    }

    @Test
    void testSendSeatHoldEmail_WithNullTimestamps() {
        // Given - event with null heldAt and expiresAt
        SeatHeldEvent eventWithNullTimes = SeatHeldEvent.builder()
                .seatId(1L)
                .seatNumber("12A")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .heldAt(null)
                .expiresAt(null)
                .timestamp(LocalDateTime.now())
                .build();

        String htmlContent = "<html>Seat Hold Email</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendSeatHoldEmail(eventWithNullTimes);

        // Then - Should succeed, showing "N/A" for null timestamps
        verify(templateService, times(1)).renderTemplate(eq("email/seat-hold"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendPaymentConfirmationEmail_WithNullTimestamp() {
        // Given - event with null timestamp
        PaymentEvent eventWithNullTime = PaymentEvent.builder()
                .paymentReference("PAY123456")
                .passengerId("P12345")
                .bookingReference("SKY123456")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .timestamp(null)
                .eventType("PAYMENT_COMPLETED")
                .build();

        String htmlContent = "<html>Payment Confirmation</html>";
        when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(htmlContent);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // When
        notificationService.sendPaymentConfirmationEmail(eventWithNullTime);

        // Then
        verify(templateService, times(1)).renderTemplate(eq("email/payment-confirmation"), anyMap());
        verify(mockEmailService, times(1)).sendEmail(anyString(), anyString(), eq(htmlContent));
    }
}
