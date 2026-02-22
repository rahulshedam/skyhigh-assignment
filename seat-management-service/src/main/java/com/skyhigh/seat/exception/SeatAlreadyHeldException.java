package com.skyhigh.seat.exception;

/**
 * Exception thrown when a seat is already held by another passenger.
 */
public class SeatAlreadyHeldException extends RuntimeException {
    public SeatAlreadyHeldException(String message) {
        super(message);
    }

    public SeatAlreadyHeldException(Long seatId) {
        super("Seat is already held by another passenger: " + seatId);
    }
}
