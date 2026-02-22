package com.skyhigh.baggage.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    private BaggageEvent testEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "baggageExchange", "baggage.exchange");
        ReflectionTestUtils.setField(eventPublisher, "baggageValidatedRoutingKey", "baggage.validated");
        ReflectionTestUtils.setField(eventPublisher, "excessFeeRoutingKey", "baggage.excess-fee");

        testEvent = BaggageEvent.builder()
                .baggageReference("BAG-12345678")
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("30.0"))
                .excessWeight(new BigDecimal("5.0"))
                .excessFee(new BigDecimal("50.0"))
                .status("EXCESS_FEE_REQUIRED")
                .timestamp(LocalDateTime.now())
                .eventType("BAGGAGE_VALIDATED")
                .build();
    }

    @Test
    void publishBaggageValidated_SendsEventToRabbitMQ() {
        // Act
        eventPublisher.publishBaggageValidated(testEvent);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("baggage.exchange"),
                eq("baggage.validated"),
                any(BaggageEvent.class));
    }

    @Test
    void publishExcessFee_SendsEventToRabbitMQ() {
        // Act
        eventPublisher.publishExcessFee(testEvent);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("baggage.exchange"),
                eq("baggage.excess-fee"),
                any(BaggageEvent.class));
    }
}
