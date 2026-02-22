package com.skyhigh.baggage.exception;

import com.skyhigh.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when baggage record is not found.
 */
public class BaggageNotFoundException extends BaseException {

    public static final String ERROR_CODE = "BAGGAGE_NOT_FOUND";

    public BaggageNotFoundException(String message) {
        super(ERROR_CODE, HttpStatus.NOT_FOUND, message);
    }

    public static BaggageNotFoundException byReference(String reference) {
        return new BaggageNotFoundException(
                String.format("Baggage record not found with reference: %s", reference));
    }
}
