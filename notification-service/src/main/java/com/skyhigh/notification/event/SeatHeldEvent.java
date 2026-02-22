package com.skyhigh.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a seat is held.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHeldEvent {

    private Long seatId;
    private String seatNumber;
    private Long flightId;
    private String flightNumber;
    private String passengerId;
    private String bookingReference;
    private LocalDateTime heldAt;
    private LocalDateTime expiresAt;
    private LocalDateTime timestamp;
}
