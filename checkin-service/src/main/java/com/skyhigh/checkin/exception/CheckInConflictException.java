package com.skyhigh.checkin.exception;

/**
 * Thrown when a seat operation conflicts with current state (e.g. seat already held, hold expired).
 * Mapped to HTTP 409 Conflict.
 */
public class CheckInConflictException extends RuntimeException {
    public CheckInConflictException(String message) {
        super(message);
    }

    public CheckInConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
