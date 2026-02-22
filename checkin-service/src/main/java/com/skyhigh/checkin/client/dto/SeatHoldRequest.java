package com.skyhigh.checkin.client.dto;

/**
 * Request to hold a seat
 */
public record SeatHoldRequest(
        String passengerId,
        String bookingReference) {
}
