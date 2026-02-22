package com.skyhigh.payment.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for publishing payment events to RabbitMQ.
 */
@Slf4j
@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.routing-keys.payment-success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-keys.payment-failure}")
    private String paymentFailureRoutingKey;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish payment completed event
     */
    public void publishPaymentCompletedEvent(PaymentEvent event) {
        try {
            event.setEventType("PAYMENT_COMPLETED");
            rabbitTemplate.convertAndSend(paymentExchange, paymentSuccessRoutingKey, event);
            log.info("Published PaymentCompletedEvent for payment: {}", event.getPaymentReference());
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent for payment: {}",
                    event.getPaymentReference(), e);
        }
    }

    /**
     * Publish payment failed event
     */
    public void publishPaymentFailedEvent(PaymentEvent event) {
        try {
            event.setEventType("PAYMENT_FAILED");
            rabbitTemplate.convertAndSend(paymentExchange, paymentFailureRoutingKey, event);
            log.info("Published PaymentFailedEvent for payment: {}", event.getPaymentReference());
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent for payment: {}",
                    event.getPaymentReference(), e);
        }
    }
}
