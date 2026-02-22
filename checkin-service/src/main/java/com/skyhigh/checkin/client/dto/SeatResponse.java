package com.skyhigh.checkin.client.dto;

/**
 * Response from Seat Management Service
 */
public record SeatResponse(
        Long id,
        String seatNumber,
        String status,
        String passengerId,
        String bookingReference) {
}
