package com.skyhigh.checkin.model.enums;

/**
 * Check-in status enumeration
 */
public enum CheckInStatus {
    /**
     * Check-in process has started
     */
    IN_PROGRESS,

    /**
     * Baggage weight exceeds 25kg, waiting for payment
     */
    WAITING_FOR_PAYMENT,

    /**
     * Check-in completed successfully
     */
    COMPLETED,

    /**
     * Check-in cancelled by passenger or system
     */
    CANCELLED
}
