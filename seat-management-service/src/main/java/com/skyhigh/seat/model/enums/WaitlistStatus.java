package com.skyhigh.seat.model.enums;

/**
 * Enum representing the status of a waitlist entry.
 */
public enum WaitlistStatus {
    /**
     * Passenger is waiting for seat to become available
     */
    WAITING,

    /**
     * Seat has been assigned to the waitlisted passenger
     */
    ASSIGNED,

    /**
     * Waitlist entry has expired (passenger didn't respond or seat was filled)
     */
    EXPIRED
}
