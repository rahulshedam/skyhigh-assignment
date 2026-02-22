package com.skyhigh.payment.exception;

import com.skyhigh.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when payment processing fails unexpectedly.
 */
public class PaymentProcessingException extends BaseException {

    public PaymentProcessingException(String message) {
        super(
                "PAYMENT_PROCESSING_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR,
                message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(
                "PAYMENT_PROCESSING_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                cause);
    }
}
