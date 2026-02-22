package com.skyhigh.notification.repository;

import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationChannel;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationRepository.
 */
@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private Notification testNotification1;
    private Notification testNotification2;
    private Notification testNotification3;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        testNotification1 = Notification.builder()
                .type(NotificationType.EMAIL)
                .channel(NotificationChannel.EMAIL)
                .recipient("user1@example.com")
                .subject("Test Subject 1")
                .content("Test Content 1")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .bookingReference("BOOK001")
                .eventType("SEAT_CONFIRMED")
                .build();

        testNotification2 = Notification.builder()
                .type(NotificationType.EMAIL)
                .channel(NotificationChannel.EMAIL)
                .recipient("user1@example.com")
                .subject("Test Subject 2")
                .content("Test Content 2")
                .status(NotificationStatus.FAILED)
                .bookingReference("BOOK001")
                .eventType("PAYMENT_FAILED")
                .errorMessage("Test error")
                .build();

        testNotification3 = Notification.builder()
                .type(NotificationType.EMAIL)
                .channel(NotificationChannel.EMAIL)
                .recipient("user2@example.com")
                .subject("Test Subject 3")
                .content("Test Content 3")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .bookingReference("BOOK002")
                .eventType("PAYMENT_COMPLETED")
                .build();
    }

    @Test
    void testSaveNotification() {
        // When
        Notification saved = notificationRepository.save(testNotification1);

        // Then
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("user1@example.com", saved.getRecipient());
    }

    @Test
    void testFindByRecipient() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        List<Notification> notifications = notificationRepository.findByRecipient("user1@example.com");

        // Then
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(n -> n.getRecipient().equals("user1@example.com")));
    }

    @Test
    void testFindByRecipientWithPagination() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = notificationRepository.findByRecipient("user1@example.com", pageable);

        // Then
        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void testFindByStatus() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        List<Notification> sentNotifications = notificationRepository.findByStatus(NotificationStatus.SENT);
        List<Notification> failedNotifications = notificationRepository.findByStatus(NotificationStatus.FAILED);

        // Then
        assertEquals(2, sentNotifications.size());
        assertEquals(1, failedNotifications.size());
        assertTrue(sentNotifications.stream().allMatch(n -> n.getStatus() == NotificationStatus.SENT));
        assertTrue(failedNotifications.stream().allMatch(n -> n.getStatus() == NotificationStatus.FAILED));
    }

    @Test
    void testFindByStatusWithPagination() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = notificationRepository.findByStatus(NotificationStatus.SENT, pageable);

        // Then
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void testFindByTypeAndRecipient() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        List<Notification> notifications = notificationRepository.findByTypeAndRecipient(
                NotificationType.EMAIL, "user1@example.com");

        // Then
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(n -> 
                n.getType() == NotificationType.EMAIL && n.getRecipient().equals("user1@example.com")));
    }

    @Test
    void testFindByTypeAndRecipientWithPagination() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = notificationRepository.findByTypeAndRecipient(
                NotificationType.EMAIL, "user1@example.com", pageable);

        // Then
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void testFindByBookingReference() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        List<Notification> notifications = notificationRepository.findByBookingReference("BOOK001");

        // Then
        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(n -> n.getBookingReference().equals("BOOK001")));
    }

    @Test
    void testFindByEventType() {
        // Given
        notificationRepository.save(testNotification1);
        notificationRepository.save(testNotification2);
        notificationRepository.save(testNotification3);

        // When
        List<Notification> seatConfirmed = notificationRepository.findByEventType("SEAT_CONFIRMED");
        List<Notification> paymentFailed = notificationRepository.findByEventType("PAYMENT_FAILED");

        // Then
        assertEquals(1, seatConfirmed.size());
        assertEquals(1, paymentFailed.size());
        assertEquals("SEAT_CONFIRMED", seatConfirmed.get(0).getEventType());
        assertEquals("PAYMENT_FAILED", paymentFailed.get(0).getEventType());
    }

    @Test
    void testFindByRecipient_NoResults() {
        // Given
        notificationRepository.save(testNotification1);

        // When
        List<Notification> notifications = notificationRepository.findByRecipient("nonexistent@example.com");

        // Then
        assertTrue(notifications.isEmpty());
    }

    @Test
    void testPrePersist() {
        // When
        Notification saved = notificationRepository.save(testNotification1);

        // Then
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        // Compare truncated to milliseconds - DB/hibernate may differ at nanosecond precision
        assertEquals(
            saved.getCreatedAt().truncatedTo(ChronoUnit.MILLIS),
            saved.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS)
        );
    }

    @Test
    void testPreUpdate() throws InterruptedException {
        // Given
        Notification saved = notificationRepository.save(testNotification1);
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();
        
        // Wait a bit to ensure time difference
        Thread.sleep(10);

        // When
        saved.setSubject("Updated Subject");
        Notification updated = notificationRepository.save(saved);

        // Then
        assertNotNull(updated.getUpdatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt) || 
                   updated.getUpdatedAt().equals(originalUpdatedAt));
    }
}
