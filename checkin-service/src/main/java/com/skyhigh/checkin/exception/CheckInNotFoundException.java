package com.skyhigh.checkin.exception;

/**
 * Exception thrown when check-in is not found
 */
public class CheckInNotFoundException extends CheckInException {
    public CheckInNotFoundException(Long checkinId) {
        super("Check-in not found with ID: " + checkinId);
    }
}
