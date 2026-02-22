package com.skyhigh.seat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming a seat assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatConfirmRequest {

    @NotNull(message = "Passenger ID is required")
    @NotBlank(message = "Passenger ID cannot be blank")
    private String passengerId;
}
