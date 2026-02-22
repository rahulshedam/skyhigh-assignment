package com.skyhigh.baggage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyhigh.baggage.model.BaggageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for baggage validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageValidationResponse {

    private String baggageReference;

    @JsonProperty("isValid")
    private boolean isValid;

    private BigDecimal weight;
    private BigDecimal excessWeight;
    private BigDecimal excessFee;
    private BaggageStatus status;
    private String message;
}
