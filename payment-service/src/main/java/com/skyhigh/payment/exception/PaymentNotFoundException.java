package com.skyhigh.payment.exception;

import com.skyhigh.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a payment is not found.
 */
public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException(String paymentReference) {
        super(
                "PAYMENT_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                String.format("Payment not found with reference: %s", paymentReference));
    }
}
