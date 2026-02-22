package com.skyhigh.seat.service;

import com.skyhigh.seat.config.RabbitMQConfig;
import com.skyhigh.seat.model.event.SeatConfirmedEvent;
import com.skyhigh.seat.model.event.SeatHeldEvent;
import com.skyhigh.seat.model.event.SeatReleasedEvent;
import com.skyhigh.seat.model.event.WaitlistAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing seat-related events to RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish seat held event.
     */
    public void publishSeatHeldEvent(SeatHeldEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SEAT_EXCHANGE,
                    RabbitMQConfig.SEAT_HELD_ROUTING_KEY,
                    event);
            log.info("Published SeatHeldEvent for seat {} by passenger {}",
                    event.getSeatId(), event.getPassengerId());
        } catch (Exception e) {
            log.error("Failed to publish SeatHeldEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish seat confirmed event.
     */
    public void publishSeatConfirmedEvent(SeatConfirmedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SEAT_EXCHANGE,
                    RabbitMQConfig.SEAT_CONFIRMED_ROUTING_KEY,
                    event);
            log.info("Published SeatConfirmedEvent for seat {} by passenger {}",
                    event.getSeatId(), event.getPassengerId());
        } catch (Exception e) {
            log.error("Failed to publish SeatConfirmedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish seat released event.
     */
    public void publishSeatReleasedEvent(SeatReleasedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SEAT_EXCHANGE,
                    RabbitMQConfig.SEAT_RELEASED_ROUTING_KEY,
                    event);
            log.info("Published SeatReleasedEvent for seat {} (reason: {})",
                    event.getSeatId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to publish SeatReleasedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish waitlist assigned event.
     */
    public void publishWaitlistAssignedEvent(WaitlistAssignedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SEAT_EXCHANGE,
                    RabbitMQConfig.WAITLIST_ASSIGNED_ROUTING_KEY,
                    event);
            log.info("Published WaitlistAssignedEvent for seat {} to passenger {}",
                    event.getSeatId(), event.getPassengerId());
        } catch (Exception e) {
            log.error("Failed to publish WaitlistAssignedEvent: {}", e.getMessage(), e);
        }
    }
}
