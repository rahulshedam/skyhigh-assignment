package com.skyhigh.checkin.client.dto;

/**
 * Request to confirm a seat
 */
public record SeatConfirmRequest(
        String passengerId,
        String bookingReference) {
}
