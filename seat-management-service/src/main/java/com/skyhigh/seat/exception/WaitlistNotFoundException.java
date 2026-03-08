package com.skyhigh.seat.exception;

/**
 * Thrown when a waitlist entry is not found (e.g. on remove or get by id).
 * Mapped to HTTP 404 Not Found.
 */
public class WaitlistNotFoundException extends RuntimeException {

    public WaitlistNotFoundException(String message) {
        super(message);
    }

    public WaitlistNotFoundException(Long waitlistId) {
        super("Waitlist entry not found: " + waitlistId);
    }
}
