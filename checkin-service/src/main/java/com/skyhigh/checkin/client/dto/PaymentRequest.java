package com.skyhigh.checkin.client.dto;

/**
 * Request to process payment
 */
public record PaymentRequest(
        String bookingReference,
        String passengerId,
        Double amount,
        String description) {
}
