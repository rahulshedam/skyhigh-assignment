package com.skyhigh.payment.dto;

import com.skyhigh.payment.model.PaymentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment request DTO for processing payments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Passenger ID is required")
    @Size(max = 50, message = "Passenger ID must not exceed 50 characters")
    private String passengerId;

    @NotBlank(message = "Booking reference is required")
    @Size(max = 10, message = "Booking reference must not exceed 10 characters")
    private String bookingReference;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g., USD)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase 3-letter code")
    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    /**
     * Flag to simulate payment failure for testing purposes.
     * When true, payment will fail with INSUFFICIENT_FUNDS error.
     * When false or null, payment will succeed.
     */
    private Boolean simulateFailure;

    /**
     * Additional metadata (optional)
     */
    @Size(max = 500, message = "Metadata must not exceed 500 characters")
    private String metadata;
}
