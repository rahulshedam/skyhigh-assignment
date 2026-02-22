package com.skyhigh.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for MockEmailService.
 */
@ExtendWith(MockitoExtension.class)
class MockEmailServiceTest {

    @InjectMocks
    private MockEmailService mockEmailService;

    @BeforeEach
    void setUp() {
        // Set private field values
        ReflectionTestUtils.setField(mockEmailService, "fromEmail", "test@skyhigh.com");
        ReflectionTestUtils.setField(mockEmailService, "mockEnabled", true);
        ReflectionTestUtils.setField(mockEmailService, "logToConsole", true);
    }

    @Test
    void testSendEmail_Success() {
        // Given
        String to = "passenger@example.com";
        String subject = "Test Email";
        String htmlContent = "<html><body>Test Content</body></html>";

        // When & Then
        assertDoesNotThrow(() -> mockEmailService.sendEmail(to, subject, htmlContent));
    }

    @Test
    void testSendPlainTextEmail_Success() {
        // Given
        String to = "passenger@example.com";
        String subject = "Test Email";
        String textContent = "Test Content";

        // When & Then
        assertDoesNotThrow(() -> mockEmailService.sendPlainTextEmail(to, subject, textContent));
    }

    @Test
    void testSendEmail_WithNullContent() {
        // Given
        String to = "passenger@example.com";
        String subject = "Test Email";
        String htmlContent = null;

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> mockEmailService.sendEmail(to, subject, htmlContent));
    }

    @Test
    void testSendEmail_WithEmptyContent() {
        // Given
        String to = "passenger@example.com";
        String subject = "Test Email";
        String htmlContent = "";

        // When & Then
        assertDoesNotThrow(() -> mockEmailService.sendEmail(to, subject, htmlContent));
    }

    @Test
    void testSendEmail_LogToConsoleDisabled() {
        // Given
        ReflectionTestUtils.setField(mockEmailService, "logToConsole", false);
        String to = "passenger@example.com";
        String subject = "Test Email";
        String htmlContent = "<html><body>Content</body></html>";

        // When & Then - Should not throw even with logging disabled
        assertDoesNotThrow(() -> mockEmailService.sendEmail(to, subject, htmlContent));
    }

    @Test
    void testSendPlainTextEmail_LogToConsoleDisabled() {
        // Given
        ReflectionTestUtils.setField(mockEmailService, "logToConsole", false);
        String to = "passenger@example.com";
        String subject = "Test Email";
        String textContent = "Plain text content";

        // When & Then
        assertDoesNotThrow(() -> mockEmailService.sendPlainTextEmail(to, subject, textContent));
    }
}
