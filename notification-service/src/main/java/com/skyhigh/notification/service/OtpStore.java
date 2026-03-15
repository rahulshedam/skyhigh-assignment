package com.skyhigh.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store with TTL.
 * For assignment purposes the OTP is always 123123.
 */
@Component
@Slf4j
public class OtpStore {

    private static final String FIXED_OTP = "123123";
    private static final long TTL_SECONDS = 300; // 5 minutes

    private record OtpEntry(String otp, Instant expiresAt) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();

    /**
     * Generate and store an OTP for the given email.
     * Always returns the fixed OTP for assignment purposes.
     */
    public String generateOtp(String email) {
        OtpEntry entry = new OtpEntry(FIXED_OTP, Instant.now().plusSeconds(TTL_SECONDS));
        store.put(email.toLowerCase(), entry);
        log.info("OTP generated for email: {} | OTP: {} | Expires: {}", email, FIXED_OTP, entry.expiresAt());
        return FIXED_OTP;
    }

    /**
     * Validate the OTP for the given email.
     */
    public boolean validateOtp(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            log.warn("OTP expired for email: {}", email);
            store.remove(email.toLowerCase());
            return false;
        }
        boolean valid = entry.otp().equals(otp);
        if (valid) {
            store.remove(email.toLowerCase()); // One-time use
            log.info("OTP validated successfully for email: {}", email);
        } else {
            log.warn("Invalid OTP attempt for email: {}", email);
        }
        return valid;
    }
}
