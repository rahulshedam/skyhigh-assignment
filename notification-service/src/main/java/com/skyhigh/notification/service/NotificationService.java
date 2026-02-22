package com.skyhigh.notification.service;

import com.skyhigh.notification.event.*;
import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationChannel;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.model.NotificationType;
import com.skyhigh.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing notifications and sending emails.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MockEmailService mockEmailService;
    private final TemplateService templateService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

    /**
     * Send seat confirmation email.
     */
    @Transactional
    public void sendSeatConfirmationEmail(SeatConfirmedEvent event) {
        try {
            log.info("Processing seat confirmation notification for passenger: {}", event.getPassengerId());

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("passengerId", event.getPassengerId());
            variables.put("bookingReference", event.getBookingReference());
            variables.put("seatNumber", event.getSeatNumber());
            variables.put("flightNumber", event.getFlightNumber());
            variables.put("flightId", event.getFlightId());
            variables.put("confirmedAt", event.getConfirmedAt() != null ? 
                event.getConfirmedAt().format(DATETIME_FORMATTER) : "N/A");
            variables.put("subject", "SkyHigh Airlines - Seat Confirmation");
            variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

            // Render template
            String htmlContent = templateService.renderTemplate("email/seat-confirmation", variables);

            // Send email
            String recipientEmail = getEmailFromPassengerId(event.getPassengerId());
            mockEmailService.sendEmail(recipientEmail, "SkyHigh Airlines - Seat Confirmation", htmlContent);

            // Save notification to database
            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject("SkyHigh Airlines - Seat Confirmation")
                    .content(htmlContent)
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(event.getBookingReference())
                    .eventType("SEAT_CONFIRMED")
                    .build();

            notificationRepository.save(notification);
            log.info("Seat confirmation notification sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send seat confirmation email for passenger: {}", event.getPassengerId(), e);
            saveFailedNotification(event.getPassengerId(), "Seat Confirmation", "SEAT_CONFIRMED", 
                    event.getBookingReference(), e.getMessage());
        }
    }

    /**
     * Send seat hold notification email.
     */
    @Transactional
    public void sendSeatHoldEmail(SeatHeldEvent event) {
        try {
            log.info("Processing seat hold notification for passenger: {}", event.getPassengerId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("passengerId", event.getPassengerId());
            variables.put("bookingReference", event.getBookingReference());
            variables.put("seatNumber", event.getSeatNumber());
            variables.put("flightNumber", event.getFlightNumber());
            variables.put("flightId", event.getFlightId());
            variables.put("heldAt", event.getHeldAt() != null ? 
                event.getHeldAt().format(DATETIME_FORMATTER) : "N/A");
            variables.put("expiresAt", event.getExpiresAt() != null ? 
                event.getExpiresAt().format(DATETIME_FORMATTER) : "N/A");
            variables.put("subject", "SkyHigh Airlines - Seat Hold Confirmation");
            variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

            String htmlContent = templateService.renderTemplate("email/seat-hold", variables);
            String recipientEmail = getEmailFromPassengerId(event.getPassengerId());
            mockEmailService.sendEmail(recipientEmail, "SkyHigh Airlines - Seat Hold Confirmation", htmlContent);

            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject("SkyHigh Airlines - Seat Hold Confirmation")
                    .content(htmlContent)
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(event.getBookingReference())
                    .eventType("SEAT_HELD")
                    .build();

            notificationRepository.save(notification);
            log.info("Seat hold notification sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send seat hold email for passenger: {}", event.getPassengerId(), e);
            saveFailedNotification(event.getPassengerId(), "Seat Hold Confirmation", "SEAT_HELD", 
                    event.getBookingReference(), e.getMessage());
        }
    }

    /**
     * Send payment confirmation email.
     */
    @Transactional
    public void sendPaymentConfirmationEmail(PaymentEvent event) {
        try {
            log.info("Processing payment confirmation notification for passenger: {}", event.getPassengerId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("passengerId", event.getPassengerId());
            variables.put("bookingReference", event.getBookingReference());
            variables.put("paymentReference", event.getPaymentReference());
            variables.put("amount", event.getAmount().toString());
            variables.put("currency", event.getCurrency());
            variables.put("timestamp", event.getTimestamp() != null ? 
                event.getTimestamp().format(DATETIME_FORMATTER) : "N/A");
            variables.put("subject", "SkyHigh Airlines - Payment Confirmation");
            variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

            String htmlContent = templateService.renderTemplate("email/payment-confirmation", variables);
            String recipientEmail = getEmailFromPassengerId(event.getPassengerId());
            mockEmailService.sendEmail(recipientEmail, "SkyHigh Airlines - Payment Confirmation", htmlContent);

            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject("SkyHigh Airlines - Payment Confirmation")
                    .content(htmlContent)
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(event.getBookingReference())
                    .eventType("PAYMENT_COMPLETED")
                    .build();

            notificationRepository.save(notification);
            log.info("Payment confirmation notification sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send payment confirmation email for passenger: {}", event.getPassengerId(), e);
            saveFailedNotification(event.getPassengerId(), "Payment Confirmation", "PAYMENT_COMPLETED", 
                    event.getBookingReference(), e.getMessage());
        }
    }

    /**
     * Send payment failure email.
     */
    @Transactional
    public void sendPaymentFailureEmail(PaymentEvent event) {
        try {
            log.info("Processing payment failure notification for passenger: {}", event.getPassengerId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("passengerId", event.getPassengerId());
            variables.put("bookingReference", event.getBookingReference());
            variables.put("paymentReference", event.getPaymentReference());
            variables.put("amount", event.getAmount().toString());
            variables.put("currency", event.getCurrency());
            variables.put("timestamp", event.getTimestamp() != null ? 
                event.getTimestamp().format(DATETIME_FORMATTER) : "N/A");
            variables.put("subject", "SkyHigh Airlines - Payment Failed");
            variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

            String htmlContent = templateService.renderTemplate("email/payment-failure", variables);
            String recipientEmail = getEmailFromPassengerId(event.getPassengerId());
            mockEmailService.sendEmail(recipientEmail, "SkyHigh Airlines - Payment Failed", htmlContent);

            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject("SkyHigh Airlines - Payment Failed")
                    .content(htmlContent)
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(event.getBookingReference())
                    .eventType("PAYMENT_FAILED")
                    .build();

            notificationRepository.save(notification);
            log.info("Payment failure notification sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send payment failure email for passenger: {}", event.getPassengerId(), e);
            saveFailedNotification(event.getPassengerId(), "Payment Failed", "PAYMENT_FAILED", 
                    event.getBookingReference(), e.getMessage());
        }
    }

    /**
     * Send waitlist assigned notification email.
     */
    @Transactional
    public void sendWaitlistNotificationEmail(WaitlistAssignedEvent event) {
        try {
            log.info("Processing waitlist assigned notification for passenger: {}", event.getPassengerId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("passengerId", event.getPassengerId());
            variables.put("bookingReference", event.getBookingReference());
            variables.put("seatNumber", event.getSeatNumber());
            variables.put("flightNumber", event.getFlightNumber());
            variables.put("flightId", event.getFlightId());
            variables.put("assignedAt", event.getAssignedAt() != null ? 
                event.getAssignedAt().format(DATETIME_FORMATTER) : "N/A");
            variables.put("expiresAt", event.getExpiresAt() != null ? 
                event.getExpiresAt().format(DATETIME_FORMATTER) : "N/A");
            variables.put("subject", "SkyHigh Airlines - Seat Assigned from Waitlist");
            variables.put("year", String.valueOf(LocalDateTime.now().getYear()));

            String htmlContent = templateService.renderTemplate("email/waitlist-assigned", variables);
            String recipientEmail = getEmailFromPassengerId(event.getPassengerId());
            mockEmailService.sendEmail(recipientEmail, "SkyHigh Airlines - Seat Assigned from Waitlist", htmlContent);

            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject("SkyHigh Airlines - Seat Assigned from Waitlist")
                    .content(htmlContent)
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(event.getBookingReference())
                    .eventType("WAITLIST_ASSIGNED")
                    .build();

            notificationRepository.save(notification);
            log.info("Waitlist notification sent successfully to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send waitlist notification email for passenger: {}", event.getPassengerId(), e);
            saveFailedNotification(event.getPassengerId(), "Waitlist Assigned", "WAITLIST_ASSIGNED", 
                    event.getBookingReference(), e.getMessage());
        }
    }

    /**
     * Convert passenger ID to email address.
     * In a real application, this would query a user service or database.
     */
    private String getEmailFromPassengerId(String passengerId) {
        // For mock purposes, generate email from passenger ID
        if (passengerId.contains("@")) {
            return passengerId; // Already an email
        }
        return passengerId.toLowerCase() + "@passenger.skyhigh.com";
    }

    /**
     * Save a failed notification to the database.
     */
    private void saveFailedNotification(String passengerId, String subject, String eventType, 
                                       String bookingReference, String errorMessage) {
        try {
            String recipientEmail = getEmailFromPassengerId(passengerId);
            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(recipientEmail)
                    .subject(subject)
                    .content("Failed to render or send notification")
                    .status(NotificationStatus.FAILED)
                    .bookingReference(bookingReference)
                    .eventType(eventType)
                    .errorMessage(errorMessage)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to save failed notification record", e);
        }
    }
}
