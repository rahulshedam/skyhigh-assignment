package com.skyhigh.seat.controller;

import com.skyhigh.seat.model.dto.SeatMapResponse;
import com.skyhigh.seat.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for seat map operations.
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seat Map", description = "APIs for retrieving seat maps")
@CrossOrigin
public class SeatMapController {

    private final SeatMapService seatMapService;

    @GetMapping("/flights/{flightId}/seatmap")
    @Operation(summary = "Get seat map", description = "Get complete seat map for a flight with availability status")
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable Long flightId) {
        log.info("Get seat map request for flight {}", flightId);
        SeatMapResponse response = seatMapService.getSeatMap(flightId);
        return ResponseEntity.ok(response);
    }
}
