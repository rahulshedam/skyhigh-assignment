package com.skyhigh.checkin.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Response from Baggage Service
 */
public record BaggageValidationResponse(
        String baggageReference,
        @JsonProperty("isValid")
        boolean isValid,
        BigDecimal weight,
        BigDecimal excessWeight,
        BigDecimal excessFee,
        String status,
        String message) {
    
    /**
     * Helper method to check if payment is required
     */
    public boolean requiresPayment() {
        return excessFee != null && excessFee.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Helper method to get excess fee as double
     */
    public Double excessFeeAsDouble() {
        return excessFee != null ? excessFee.doubleValue() : 0.0;
    }
}
