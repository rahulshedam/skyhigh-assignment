package com.skyhigh.seat.model.dto;

import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for seat information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private String seatNumber;
    private Long flightId;
    private SeatClass seatClass;
    private SeatStatus status;
    private String passengerId;
    private String bookingReference;
}
