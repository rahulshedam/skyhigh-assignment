package com.skyhigh.seat.exception;

/**
 * Exception thrown when a seat is not found.
 */
public class SeatNotFoundException extends RuntimeException {
    public SeatNotFoundException(String message) {
        super(message);
    }

    public SeatNotFoundException(Long seatId) {
        super("Seat not found with ID: " + seatId);
    }
}
