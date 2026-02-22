package com.skyhigh.checkin.exception;

/**
 * Base exception for check-in service
 */
public class CheckInException extends RuntimeException {
    public CheckInException(String message) {
        super(message);
    }

    public CheckInException(String message, Throwable cause) {
        super(message, cause);
    }
}
