package com.skyhigh.checkin.client.dto;

import java.math.BigDecimal;

/**
 * Request to validate baggage
 */
public record BaggageValidationRequest(
        String passengerId,
        String bookingReference,
        BigDecimal weight) {
}
