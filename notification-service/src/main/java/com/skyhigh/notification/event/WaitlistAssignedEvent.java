package com.skyhigh.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a waitlisted passenger is assigned a seat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistAssignedEvent {

    private Long waitlistId;
    private Long seatId;
    private String seatNumber;
    private Long flightId;
    private String flightNumber;
    private String passengerId;
    private String bookingReference;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime timestamp;
}
