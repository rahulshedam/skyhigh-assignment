package com.skyhigh.notification.repository;

import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing Notification entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find notifications by recipient.
     */
    List<Notification> findByRecipient(String recipient);

    /**
     * Find notifications by recipient with pagination.
     */
    Page<Notification> findByRecipient(String recipient, Pageable pageable);

    /**
     * Find notifications by status.
     */
    List<Notification> findByStatus(NotificationStatus status);

    /**
     * Find notifications by status with pagination.
     */
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Find notifications by type and recipient.
     */
    List<Notification> findByTypeAndRecipient(NotificationType type, String recipient);

    /**
     * Find notifications by type and recipient with pagination.
     */
    Page<Notification> findByTypeAndRecipient(NotificationType type, String recipient, Pageable pageable);

    /**
     * Find notifications by booking reference.
     */
    List<Notification> findByBookingReference(String bookingReference);

    /**
     * Find notifications by event type.
     */
    List<Notification> findByEventType(String eventType);
}
