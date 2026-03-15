package com.skyhigh.seat.exception;

/**
 * Exception thrown when a seat operation conflicts with the current seat holder.
 * For example: attempting to confirm or cancel a seat held by a different passenger.
 * Mapped to HTTP 409 Conflict.
 */
public class SeatConflictException extends RuntimeException {
    public SeatConflictException(String message) {
        super(message);
    }
}
