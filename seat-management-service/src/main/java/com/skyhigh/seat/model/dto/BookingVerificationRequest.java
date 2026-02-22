package com.skyhigh.seat.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to verify a booking reference
 */
public record BookingVerificationRequest(
        @NotBlank(message = "Booking reference is required")
        String bookingReference,
        
        @NotBlank(message = "Passenger ID is required")
        String passengerId
) {}
