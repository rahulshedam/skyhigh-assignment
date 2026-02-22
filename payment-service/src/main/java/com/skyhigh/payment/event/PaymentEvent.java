package com.skyhigh.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base event for payment-related events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String paymentReference;
    private String passengerId;
    private String bookingReference;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private String eventType;
    private String metadata;
}
