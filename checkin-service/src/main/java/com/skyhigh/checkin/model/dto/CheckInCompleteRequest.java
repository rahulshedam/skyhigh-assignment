package com.skyhigh.checkin.model.dto;

/**
 * Request to complete check-in (after payment if required)
 */
public record CheckInCompleteRequest(
        String paymentId // Optional, only required if payment was needed
) {
}
