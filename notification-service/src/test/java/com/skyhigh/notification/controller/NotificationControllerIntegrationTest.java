package com.skyhigh.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.notification.dto.NotificationRequest;
import com.skyhigh.notification.model.Notification;
import com.skyhigh.notification.model.NotificationChannel;
import com.skyhigh.notification.model.NotificationStatus;
import com.skyhigh.notification.model.NotificationType;
import com.skyhigh.notification.repository.NotificationRepository;
import com.skyhigh.notification.service.MockEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MockEmailService mockEmailService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        notificationRepository.deleteAll();

        // Setup test notification
        testNotification = Notification.builder()
                .type(NotificationType.EMAIL)
                .channel(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Notification")
                .content("Test Content")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .bookingReference("TEST123")
                .eventType("TEST_EVENT")
                .build();

        // Mock email service
        doNothing().when(mockEmailService).sendPlainTextEmail(any(), any(), any());
    }

    @Test
    void testGetAllNotifications_Success() throws Exception {
        // Given
        notificationRepository.save(testNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications[0].subject").value("Test Notification"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void testGetNotificationById_Success() throws Exception {
        // Given
        Notification saved = notificationRepository.save(testNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.subject").value("Test Notification"));
    }

    @Test
    void testGetNotificationById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notifications/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetNotificationsByRecipient_Success() throws Exception {
        // Given
        notificationRepository.save(testNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications/recipient/{recipientId}", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].recipient").value("test@example.com"));
    }

    @Test
    void testGetNotificationsByStatus_Success() throws Exception {
        // Given
        notificationRepository.save(testNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications/status/{status}", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void testGetNotificationsByBookingReference_Success() throws Exception {
        // Given
        notificationRepository.save(testNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications/booking/{bookingReference}", "TEST123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].bookingReference").value("TEST123"));
    }

    @Test
    void testSendTestNotification_Success() throws Exception {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .bookingReference("TEST123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/notifications/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipient").value("test@example.com"))
                .andExpect(jsonPath("$.subject").value("Test Subject"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void testSendTestNotification_InvalidEmail() throws Exception {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipient("invalid-email")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        // When & Then
        mockMvc.perform(post("/api/notifications/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendTestNotification_MissingFields() throws Exception {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .recipient("test@example.com")
                .build();

        // When & Then
        mockMvc.perform(post("/api/notifications/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetNotificationStats_Success() throws Exception {
        // Given
        notificationRepository.save(testNotification);
        
        Notification failedNotification = Notification.builder()
                .type(NotificationType.EMAIL)
                .channel(NotificationChannel.EMAIL)
                .recipient("failed@example.com")
                .subject("Failed Notification")
                .content("Failed Content")
                .status(NotificationStatus.FAILED)
                .bookingReference("FAIL123")
                .eventType("TEST_EVENT")
                .errorMessage("Test error")
                .build();
        notificationRepository.save(failedNotification);

        // When & Then
        mockMvc.perform(get("/api/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.pending").value(0));
    }

    @Test
    void testHelloEndpoint() throws Exception {
        mockMvc.perform(get("/api/notifications/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from Notification Service!"));
    }
}
