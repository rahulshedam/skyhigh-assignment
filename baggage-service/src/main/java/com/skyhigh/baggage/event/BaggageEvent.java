package com.skyhigh.baggage.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when baggage is validated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageEvent {

    private String baggageReference;
    private String passengerId;
    private String bookingReference;
    private BigDecimal weight;
    private BigDecimal excessWeight;
    private BigDecimal excessFee;
    private String status;
    private LocalDateTime timestamp;
    private String eventType;
}
