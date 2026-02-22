package com.skyhigh.payment.dto;

import com.skyhigh.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment data DTO for responses.
 * This will be wrapped in ApiResponse<PaymentData> for consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentData {

    private String paymentReference;
    private PaymentStatus status;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private LocalDateTime timestamp;
}
