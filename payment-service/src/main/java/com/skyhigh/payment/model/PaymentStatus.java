package com.skyhigh.payment.model;

/**
 * Payment status enumeration.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but not yet processed
     */
    PENDING,

    /**
     * Payment is currently being processed
     */
    PROCESSING,

    /**
     * Payment completed successfully
     */
    COMPLETED,

    /**
     * Payment failed
     */
    FAILED
}
