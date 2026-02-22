package com.skyhigh.notification.controller;

import com.skyhigh.notification.dto.NotificationRequest;
import com.skyhigh.notification.dto.NotificationResponse;
import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationChannel;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.model.NotificationType;
import com.skyhigh.notification.repository.NotificationRepository;
import com.skyhigh.notification.service.MockEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for managing and querying notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final MockEmailService mockEmailService;

    @GetMapping("/hello")
    @Operation(summary = "Health check endpoint")
    public String hello() {
        return "Hello from Notification Service!";
    }

    /**
     * Get all notifications with pagination.
     */
    @GetMapping
    @Operation(summary = "Get all notifications", description = "Retrieve all notifications with pagination")
    public ResponseEntity<Map<String, Object>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Notification> notificationsPage = notificationRepository.findAll(pageable);
            
            List<NotificationResponse> notifications = notificationsPage.getContent()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("currentPage", notificationsPage.getNumber());
            response.put("totalItems", notificationsPage.getTotalElements());
            response.put("totalPages", notificationsPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get notification by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(notification -> ResponseEntity.ok(convertToResponse(notification)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get notifications by recipient.
     */
    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get notifications by recipient", description = "Retrieve all notifications for a specific recipient")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByRecipient(
            @PathVariable String recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Notification> notificationsPage = notificationRepository.findByRecipient(recipientId, pageable);
            
            List<NotificationResponse> notifications = notificationsPage.getContent()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching notifications for recipient: {}", recipientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get notifications by status.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get notifications by status", description = "Retrieve all notifications with a specific status")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByStatus(
            @PathVariable NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Notification> notificationsPage = notificationRepository.findByStatus(status, pageable);
            
            List<NotificationResponse> notifications = notificationsPage.getContent()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching notifications by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get notifications by booking reference.
     */
    @GetMapping("/booking/{bookingReference}")
    @Operation(summary = "Get notifications by booking reference", description = "Retrieve all notifications for a booking")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByBookingReference(
            @PathVariable String bookingReference) {
        
        try {
            List<Notification> notifications = notificationRepository.findByBookingReference(bookingReference);
            
            List<NotificationResponse> response = notifications.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notifications for booking: {}", bookingReference, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a test notification (for demo/testing purposes).
     */
    @PostMapping("/test")
    @Operation(summary = "Send test notification", description = "Send a test email notification (for demo purposes)")
    public ResponseEntity<NotificationResponse> sendTestNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        try {
            log.info("Sending test notification to: {}", request.getRecipient());
            
            // Send mock email
            mockEmailService.sendPlainTextEmail(
                    request.getRecipient(),
                    request.getSubject(),
                    request.getContent()
            );
            
            // Save notification
            Notification notification = Notification.builder()
                    .type(NotificationType.EMAIL)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(request.getRecipient())
                    .subject(request.getSubject())
                    .content(request.getContent())
                    .status(NotificationStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .bookingReference(request.getBookingReference())
                    .eventType("TEST")
                    .build();
            
            Notification savedNotification = notificationRepository.save(notification);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(convertToResponse(savedNotification));
        } catch (Exception e) {
            log.error("Error sending test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get notification statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics", description = "Get overall statistics about notifications")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        try {
            long total = notificationRepository.count();
            long sent = notificationRepository.findByStatus(NotificationStatus.SENT).size();
            long failed = notificationRepository.findByStatus(NotificationStatus.FAILED).size();
            long pending = notificationRepository.findByStatus(NotificationStatus.PENDING).size();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("sent", sent);
            stats.put("failed", failed);
            stats.put("pending", pending);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching notification statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert Notification entity to NotificationResponse DTO.
     */
    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .bookingReference(notification.getBookingReference())
                .eventType(notification.getEventType())
                .errorMessage(notification.getErrorMessage())
                .build();
    }
}
