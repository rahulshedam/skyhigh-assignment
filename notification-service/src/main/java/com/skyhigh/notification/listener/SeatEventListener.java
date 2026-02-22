package com.skyhigh.notification.listener;

import com.skyhigh.notification.event.SeatConfirmedEvent;
import com.skyhigh.notification.event.SeatHeldEvent;
import com.skyhigh.notification.event.SeatReleasedEvent;
import com.skyhigh.notification.event.WaitlistAssignedEvent;
import com.skyhigh.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener for seat-related events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeatEventListener {

    private final NotificationService notificationService;

    /**
     * Listen for seat confirmed events.
     */
    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void handleSeatConfirmedEvent(SeatConfirmedEvent event) {
        log.info("Received SeatConfirmedEvent for passenger: {}, booking: {}", 
                event.getPassengerId(), event.getBookingReference());
        
        try {
            notificationService.sendSeatConfirmationEmail(event);
        } catch (Exception e) {
            log.error("Error processing SeatConfirmedEvent", e);
            // RabbitMQ will retry based on configuration
            throw new RuntimeException("Failed to process seat confirmed event", e);
        }
    }

    /**
     * Listen for seat held events.
     */
    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void handleSeatHeldEvent(SeatHeldEvent event) {
        log.info("Received SeatHeldEvent for passenger: {}, booking: {}", 
                event.getPassengerId(), event.getBookingReference());
        
        try {
            notificationService.sendSeatHoldEmail(event);
        } catch (Exception e) {
            log.error("Error processing SeatHeldEvent", e);
            throw new RuntimeException("Failed to process seat held event", e);
        }
    }

    /**
     * Listen for seat released events.
     * Note: We might not send email for all seat releases, but log them.
     */
    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void handleSeatReleasedEvent(SeatReleasedEvent event) {
        log.info("Received SeatReleasedEvent for passenger: {}, seat: {}, reason: {}", 
                event.getPassengerId(), event.getSeatNumber(), event.getReason());
        
        // For now, just log. Could send cancellation email if needed.
        log.debug("Seat released event processed (no notification sent)");
    }

    /**
     * Listen for waitlist assigned events.
     */
    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void handleWaitlistAssignedEvent(WaitlistAssignedEvent event) {
        log.info("Received WaitlistAssignedEvent for passenger: {}, booking: {}", 
                event.getPassengerId(), event.getBookingReference());
        
        try {
            notificationService.sendWaitlistNotificationEmail(event);
        } catch (Exception e) {
            log.error("Error processing WaitlistAssignedEvent", e);
            throw new RuntimeException("Failed to process waitlist assigned event", e);
        }
    }
}
