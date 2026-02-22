package com.skyhigh.baggage.exception;

import com.skyhigh.common.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when invalid weight is provided.
 */
public class InvalidWeightException extends BaseException {

    public static final String ERROR_CODE = "INVALID_WEIGHT";

    public InvalidWeightException(String message) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, message);
    }
}
