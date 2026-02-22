package com.skyhigh.seat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing booking verification details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingVerificationResponse {
    private Long flightId;
    private String flightNumber;
    private String origin;
    private String destination;
    private String bookingReference;
    private String passengerId;
    private boolean valid;
}
