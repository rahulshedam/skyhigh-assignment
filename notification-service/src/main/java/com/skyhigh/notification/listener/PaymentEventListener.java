package com.skyhigh.notification.listener;

import com.skyhigh.notification.event.PaymentEvent;
import com.skyhigh.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener for payment-related events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final NotificationService notificationService;

    /**
     * Listen for payment events (both completed and failed).
     */
    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent - Type: {}, Reference: {}, Booking: {}", 
                event.getEventType(), event.getPaymentReference(), event.getBookingReference());
        
        try {
            if ("PAYMENT_COMPLETED".equals(event.getEventType())) {
                notificationService.sendPaymentConfirmationEmail(event);
            } else if ("PAYMENT_FAILED".equals(event.getEventType())) {
                notificationService.sendPaymentFailureEmail(event);
            } else {
                log.warn("Unknown payment event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing PaymentEvent", e);
            // RabbitMQ will retry based on configuration
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
}
