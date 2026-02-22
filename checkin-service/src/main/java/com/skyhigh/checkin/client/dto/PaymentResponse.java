package com.skyhigh.checkin.client.dto;

/**
 * Response from Payment Service
 */
public record PaymentResponse(
        String paymentId,
        String status,
        Double amount,
        String message) {
}
