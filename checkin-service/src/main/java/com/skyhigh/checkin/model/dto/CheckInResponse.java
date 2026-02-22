package com.skyhigh.checkin.model.dto;

import com.skyhigh.checkin.model.enums.CheckInStatus;

import java.time.LocalDateTime;

/**
 * Check-in response DTO
 */
public record CheckInResponse(
        Long id,
        String bookingReference,
        String passengerId,
        Long flightId,
        Long seatId,
        CheckInStatus status,
        Double baggageWeight,
        Double excessBaggageFee,
        String paymentId,
        String message,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
