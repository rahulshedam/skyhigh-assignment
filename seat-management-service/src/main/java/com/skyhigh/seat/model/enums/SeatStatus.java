package com.skyhigh.seat.model.enums;

/**
 * Enum representing the status of a seat.
 */
public enum SeatStatus {
    /**
     * Seat is available for selection
     */
    AVAILABLE,

    /**
     * Seat is temporarily held by a passenger (120-second timer active)
     */
    HELD,

    /**
     * Seat is permanently assigned to a passenger after check-in completion
     */
    CONFIRMED
}
