package com.skyhigh.seat.service;

import com.skyhigh.seat.config.RabbitMQConfig;
import com.skyhigh.seat.model.event.SeatConfirmedEvent;
import com.skyhigh.seat.model.event.SeatHeldEvent;
import com.skyhigh.seat.model.event.SeatReleasedEvent;
import com.skyhigh.seat.model.event.WaitlistAssignedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceTest {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @InjectMocks
        private EventPublisherService eventPublisherService;

        private SeatHeldEvent seatHeldEvent;
        private SeatConfirmedEvent seatConfirmedEvent;
        private SeatReleasedEvent seatReleasedEvent;
        private WaitlistAssignedEvent waitlistAssignedEvent;

        @BeforeEach
        void setUp() {
                LocalDateTime now = LocalDateTime.now();

                seatHeldEvent = SeatHeldEvent.builder()
                                .seatId(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .flightNumber("SK101")
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .heldAt(now)
                                .expiresAt(now.plusSeconds(120))
                                .timestamp(now)
                                .build();

                seatConfirmedEvent = SeatConfirmedEvent.builder()
                                .seatId(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .flightNumber("SK101")
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .confirmedAt(now)
                                .timestamp(now)
                                .build();

                seatReleasedEvent = SeatReleasedEvent.builder()
                                .seatId(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .flightNumber("SK101")
                                .passengerId("PASS123")
                                .reason("CANCELLED")
                                .timestamp(now)
                                .build();

                waitlistAssignedEvent = WaitlistAssignedEvent.builder()
                                .waitlistId(1L)
                                .seatId(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .flightNumber("SK101")
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .assignedAt(now)
                                .expiresAt(now.plusSeconds(120))
                                .timestamp(now)
                                .build();
        }

        @Test
        void publishSeatHeldEvent_Success() {
                // Act
                eventPublisherService.publishSeatHeldEvent(seatHeldEvent);

                // Assert
                verify(rabbitTemplate).convertAndSend(
                                eq(RabbitMQConfig.SEAT_EXCHANGE),
                                eq(RabbitMQConfig.SEAT_HELD_ROUTING_KEY),
                                eq(seatHeldEvent));
        }

        @Test
        void publishSeatHeldEvent_RabbitTemplateThrowsException_HandlesGracefully() {
                // Arrange
                doThrow(new RuntimeException("RabbitMQ error"))
                                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

                // Act - should not throw exception
                eventPublisherService.publishSeatHeldEvent(seatHeldEvent);

                // Assert
                verify(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
        }

        @Test
        void publishSeatConfirmedEvent_Success() {
                // Act
                eventPublisherService.publishSeatConfirmedEvent(seatConfirmedEvent);

                // Assert
                verify(rabbitTemplate).convertAndSend(
                                eq(RabbitMQConfig.SEAT_EXCHANGE),
                                eq(RabbitMQConfig.SEAT_CONFIRMED_ROUTING_KEY),
                                eq(seatConfirmedEvent));
        }

        @Test
        void publishSeatReleasedEvent_Success() {
                // Act
                eventPublisherService.publishSeatReleasedEvent(seatReleasedEvent);

                // Assert
                verify(rabbitTemplate).convertAndSend(
                                eq(RabbitMQConfig.SEAT_EXCHANGE),
                                eq(RabbitMQConfig.SEAT_RELEASED_ROUTING_KEY),
                                eq(seatReleasedEvent));
        }

        @Test
        void publishWaitlistAssignedEvent_Success() {
                // Act
                eventPublisherService.publishWaitlistAssignedEvent(waitlistAssignedEvent);

                // Assert
                verify(rabbitTemplate).convertAndSend(
                                eq(RabbitMQConfig.SEAT_EXCHANGE),
                                eq(RabbitMQConfig.WAITLIST_ASSIGNED_ROUTING_KEY),
                                eq(waitlistAssignedEvent));
        }
}
