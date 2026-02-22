package com.skyhigh.seat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for seat map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {

    private Long flightId;
    private String flightNumber;
    private List<SeatResponse> seats;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer heldSeats;
    private Integer confirmedSeats;
}
