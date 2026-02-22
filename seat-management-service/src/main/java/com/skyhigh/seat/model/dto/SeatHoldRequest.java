package com.skyhigh.seat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for holding a seat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {

    @NotNull(message = "Passenger ID is required")
    @NotBlank(message = "Passenger ID cannot be blank")
    private String passengerId;

    @NotNull(message = "Booking reference is required")
    @NotBlank(message = "Booking reference cannot be blank")
    private String bookingReference;
}
