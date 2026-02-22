package com.skyhigh.seat.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a seat is released.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReleasedEvent {

    private Long seatId;
    private String seatNumber;
    private Long flightId;
    private String flightNumber;
    private String passengerId;
    private String reason; // "CANCELLED", "EXPIRED"
    private LocalDateTime timestamp;
}
