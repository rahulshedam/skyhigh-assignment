package com.skyhigh.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response structure following RFC 7807 Problem Details for
 * HTTP APIs.
 * Used by GlobalExceptionHandler to return consistent error responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status reason phrase (e.g., "Not Found", "Bad Request")
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Detailed error information (for validation errors, etc.)
     */
    private List<ErrorDetail> errors;

    /**
     * Create an error response with detailed errors
     */
    public static ErrorResponse of(int status, String error, String message, String path, List<ErrorDetail> errors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }

    /**
     * Create a simple error response without detailed errors
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return of(status, error, message, path, null);
    }
}
