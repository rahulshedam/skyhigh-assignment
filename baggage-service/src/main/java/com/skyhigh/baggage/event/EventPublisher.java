package com.skyhigh.baggage.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for publishing baggage events to RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.baggage}")
    private String baggageExchange;

    @Value("${rabbitmq.routing-keys.baggage-validated}")
    private String baggageValidatedRoutingKey;

    @Value("${rabbitmq.routing-keys.excess-fee}")
    private String excessFeeRoutingKey;

    /**
     * Publish baggage validated event.
     */
    public void publishBaggageValidated(BaggageEvent event) {
        try {
            log.info("Publishing baggage validated event for reference: {}", event.getBaggageReference());
            rabbitTemplate.convertAndSend(baggageExchange, baggageValidatedRoutingKey, event);
            log.debug("Successfully published baggage validated event");
        } catch (Exception e) {
            log.error("Failed to publish baggage validated event", e);
        }
    }

    /**
     * Publish excess fee event.
     */
    public void publishExcessFee(BaggageEvent event) {
        try {
            log.info("Publishing excess fee event for reference: {}", event.getBaggageReference());
            rabbitTemplate.convertAndSend(baggageExchange, excessFeeRoutingKey, event);
            log.debug("Successfully published excess fee event");
        } catch (Exception e) {
            log.error("Failed to publish excess fee event", e);
        }
    }
}
