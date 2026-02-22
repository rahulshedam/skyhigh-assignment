package com.skyhigh.payment.event;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    private PaymentEvent paymentEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "paymentExchange", "payment.exchange");
        ReflectionTestUtils.setField(eventPublisher, "paymentSuccessRoutingKey", "payment.completed");
        ReflectionTestUtils.setField(eventPublisher, "paymentFailureRoutingKey", "payment.failed");

        paymentEvent = PaymentEvent.builder()
                .paymentReference("PAY-123")
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void publishPaymentCompletedEvent() {
        eventPublisher.publishPaymentCompletedEvent(paymentEvent);
        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq("payment.completed"), eq(paymentEvent));
    }

    @Test
    void publishPaymentFailedEvent() {
        eventPublisher.publishPaymentFailedEvent(paymentEvent);
        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq("payment.failed"), eq(paymentEvent));
    }
}
