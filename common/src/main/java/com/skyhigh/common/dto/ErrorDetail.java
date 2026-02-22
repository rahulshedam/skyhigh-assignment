package com.skyhigh.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed error information for validation and business rule violations.
 * Used within ErrorResponse to provide field-level error details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail {

    /**
     * The field name that caused the error (e.g., "amount", "passengerId")
     * Null for non-field-specific errors
     */
    private String field;

    /**
     * Error code in UPPERCASE_SNAKE_CASE format (e.g., "INVALID_AMOUNT",
     * "NOT_NULL")
     */
    private String code;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Create an error detail for a specific field
     */
    public static ErrorDetail of(String field, String code, String message) {
        return ErrorDetail.builder()
                .field(field)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Create a general error detail without a specific field
     */
    public static ErrorDetail of(String code, String message) {
        return ErrorDetail.builder()
                .code(code)
                .message(message)
                .build();
    }
}
