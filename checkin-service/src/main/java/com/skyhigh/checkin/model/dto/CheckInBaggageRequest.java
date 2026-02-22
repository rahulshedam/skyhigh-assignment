package com.skyhigh.checkin.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request to update baggage weight
 */
public record CheckInBaggageRequest(
        @NotNull(message = "Weight is required") @PositiveOrZero(message = "Weight must be zero or positive") Double weight) {
}
