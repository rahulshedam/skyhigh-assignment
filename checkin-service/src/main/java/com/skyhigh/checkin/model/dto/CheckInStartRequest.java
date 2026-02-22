package com.skyhigh.checkin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request to start check-in process.
 * baggageWeight is optional; defaults to 0 when omitted (user enters baggage in a later step).
 */
public record CheckInStartRequest(
                @NotBlank(message = "Booking reference is required") String bookingReference,

                @NotBlank(message = "Passenger ID is required") String passengerId,

                @NotNull(message = "Flight ID is required") @Positive(message = "Flight ID must be positive") Long flightId,

                @NotNull(message = "Seat ID is required") @Positive(message = "Seat ID must be positive") Long seatId,

                @PositiveOrZero(message = "Baggage weight must be zero or positive") Double baggageWeight) {

    /**
     * Default baggageWeight to 0 when null (user hasn't entered baggage at check-in start).
     */
    public CheckInStartRequest {
        if (baggageWeight == null) {
            baggageWeight = 0.0;
        }
    }
}
