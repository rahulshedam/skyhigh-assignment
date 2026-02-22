package com.skyhigh.notification.listener;

import com.skyhigh.notification.event.SeatConfirmedEvent;
import com.skyhigh.notification.event.SeatHeldEvent;
import com.skyhigh.notification.event.SeatReleasedEvent;
import com.skyhigh.notification.event.WaitlistAssignedEvent;
import com.skyhigh.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeatEventListener.
 */
@ExtendWith(MockitoExtension.class)
class SeatEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SeatEventListener seatEventListener;

    private SeatConfirmedEvent seatConfirmedEvent;
    private SeatHeldEvent seatHeldEvent;
    private SeatReleasedEvent seatReleasedEvent;
    private WaitlistAssignedEvent waitlistAssignedEvent;

    @BeforeEach
    void setUp() {
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

        seatReleasedEvent = SeatReleasedEvent.builder()
                .seatId(1L)
                .seatNumber("12A")
                .flightId(100L)
                .flightNumber("SK123")
                .passengerId("P12345")
                .reason("EXPIRED")
                .timestamp(LocalDateTime.now())
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
    void testHandleSeatConfirmedEvent_Success() {
        // Given
        doNothing().when(notificationService).sendSeatConfirmationEmail(any(SeatConfirmedEvent.class));

        // When
        seatEventListener.handleSeatConfirmedEvent(seatConfirmedEvent);

        // Then
        verify(notificationService, times(1)).sendSeatConfirmationEmail(seatConfirmedEvent);
    }

    @Test
    void testHandleSeatConfirmedEvent_ThrowsException() {
        // Given
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendSeatConfirmationEmail(any(SeatConfirmedEvent.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                seatEventListener.handleSeatConfirmedEvent(seatConfirmedEvent));
        
        verify(notificationService, times(1)).sendSeatConfirmationEmail(seatConfirmedEvent);
    }

    @Test
    void testHandleSeatHeldEvent_Success() {
        // Given
        doNothing().when(notificationService).sendSeatHoldEmail(any(SeatHeldEvent.class));

        // When
        seatEventListener.handleSeatHeldEvent(seatHeldEvent);

        // Then
        verify(notificationService, times(1)).sendSeatHoldEmail(seatHeldEvent);
    }

    @Test
    void testHandleSeatHeldEvent_ThrowsException() {
        // Given
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendSeatHoldEmail(any(SeatHeldEvent.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                seatEventListener.handleSeatHeldEvent(seatHeldEvent));
    }

    @Test
    void testHandleWaitlistAssignedEvent_Success() {
        // Given
        doNothing().when(notificationService).sendWaitlistNotificationEmail(any(WaitlistAssignedEvent.class));

        // When
        seatEventListener.handleWaitlistAssignedEvent(waitlistAssignedEvent);

        // Then
        verify(notificationService, times(1)).sendWaitlistNotificationEmail(waitlistAssignedEvent);
    }

    @Test
    void testHandleWaitlistAssignedEvent_ThrowsException() {
        // Given
        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendWaitlistNotificationEmail(any(WaitlistAssignedEvent.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                seatEventListener.handleWaitlistAssignedEvent(waitlistAssignedEvent));
    }

    @Test
    void testHandleSeatReleasedEvent_Success() {
        // When - no notification is sent for seat released, just logged
        seatEventListener.handleSeatReleasedEvent(seatReleasedEvent);

        // Then - no notification service calls should be made
        verify(notificationService, never()).sendSeatConfirmationEmail(any());
        verify(notificationService, never()).sendSeatHoldEmail(any());
        verify(notificationService, never()).sendWaitlistNotificationEmail(any());
        verify(notificationService, never()).sendPaymentConfirmationEmail(any());
        verify(notificationService, never()).sendPaymentFailureEmail(any());
    }
}
