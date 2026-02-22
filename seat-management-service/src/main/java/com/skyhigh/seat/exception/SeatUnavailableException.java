package com.skyhigh.seat.exception;

/**
 * Exception thrown when a seat is not available for the requested operation.
 */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }

    public SeatUnavailableException(Long seatId, String currentStatus) {
        super("Seat " + seatId + " is not available. Current status: " + currentStatus);
    }
}
