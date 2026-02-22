package com.skyhigh.baggage.dto;

import com.skyhigh.baggage.model.BaggageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for baggage record data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageData {

    private String baggageReference;
    private String passengerId;
    private String bookingReference;
    private BigDecimal weight;
    private BigDecimal excessWeight;
    private BigDecimal excessFee;
    private BaggageStatus status;
    private LocalDateTime createdAt;
}
