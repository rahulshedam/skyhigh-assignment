package com.skyhigh.seat.model.enums;

/**
 * Enum representing the status of a seat assignment.
 */
public enum SeatAssignmentStatus {
    /**
     * Seat is temporarily held (120-second timer active)
     */
    HELD,

    /**
     * Seat assignment is confirmed
     */
    CONFIRMED,

    /**
     * Seat assignment was cancelled
     */
    CANCELLED
}
