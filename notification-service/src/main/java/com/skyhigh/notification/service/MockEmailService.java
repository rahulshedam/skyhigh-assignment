package com.skyhigh.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mock Email Service that logs emails to console instead of sending them.
 * Perfect for assignments and testing without requiring external SMTP configuration.
 */
@Service
@Slf4j
public class MockEmailService {

    @Value("${email.from:noreply@skyhigh-airlines.com}")
    private String fromEmail;

    @Value("${email.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${email.mock.log-to-console:true}")
    private boolean logToConsole;

    /**
     * Send a mock email (logs to console instead of actually sending).
     *
     * @param to          Recipient email address
     * @param subject     Email subject
     * @param htmlContent HTML content of the email
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        if (logToConsole) {
            logEmailToConsole(to, subject, htmlContent);
        }

        // In production, this would use JavaMailSender:
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setFrom(fromEmail);
        // helper.setTo(to);
        // helper.setSubject(subject);
        // helper.setText(htmlContent, true);
        // mailSender.send(message);

        log.debug("Mock email sent successfully to: {}", to);
    }

    /**
     * Log email details to console with clear formatting.
     */
    private void logEmailToConsole(String to, String subject, String htmlContent) {
        String separator = "=".repeat(80);
        
        log.info("\n{}", separator);
        log.info("📧 MOCK EMAIL SENT");
        log.info("{}", separator);
        log.info("From: {}", fromEmail);
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Content Type: text/html");
        log.info("\n--- HTML Content ---");
        log.info("{}", htmlContent);
        log.info("{}\n", separator);
    }

    /**
     * Send a plain text email (for non-HTML content).
     *
     * @param to          Recipient email address
     * @param subject     Email subject
     * @param textContent Plain text content of the email
     */
    public void sendPlainTextEmail(String to, String subject, String textContent) {
        if (logToConsole) {
            String separator = "=".repeat(80);
            
            log.info("\n{}", separator);
            log.info("📧 MOCK EMAIL SENT (Plain Text)");
            log.info("{}", separator);
            log.info("From: {}", fromEmail);
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Content Type: text/plain");
            log.info("\n--- Text Content ---");
            log.info("{}", textContent);
            log.info("{}\n", separator);
        }

        log.debug("Mock plain text email sent successfully to: {}", to);
    }
}
