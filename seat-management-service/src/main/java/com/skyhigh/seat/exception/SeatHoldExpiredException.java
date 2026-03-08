package com.skyhigh.seat.exception;

/**
 * Exception thrown when a seat hold has expired (TTL elapsed) and the client attempts to confirm.
 * Mapped to HTTP 409 Conflict.
 */
public class SeatHoldExpiredException extends RuntimeException {
    public SeatHoldExpiredException(String message) {
        super(message);
    }

    public SeatHoldExpiredException(Long seatId) {
        super("Seat hold has expired for seat: " + seatId);
    }
}
