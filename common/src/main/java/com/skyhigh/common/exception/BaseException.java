package com.skyhigh.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for all custom business exceptions across all
 * microservices.
 * All service-specific exceptions should extend this class.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    /**
     * Error code in UPPERCASE_SNAKE_CASE format
     */
    private final String errorCode;

    /**
     * HTTP status code to return
     */
    private final HttpStatus httpStatus;

    /**
     * Additional details about the error
     */
    private final String details;

    /**
     * Constructor with error code, HTTP status, and message
     */
    protected BaseException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }

    /**
     * Constructor with error code, HTTP status, message, and details
     */
    protected BaseException(String errorCode, HttpStatus httpStatus, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    /**
     * Constructor with error code, HTTP status, message, and cause
     */
    protected BaseException(String errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }
}
