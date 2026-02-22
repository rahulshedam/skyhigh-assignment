package com.skyhigh.seat.controller;

import com.skyhigh.seat.model.dto.SeatConfirmRequest;
import com.skyhigh.seat.model.dto.SeatHoldRequest;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for seat operations.
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seat Management", description = "APIs for seat hold, confirm, and cancel operations")
@CrossOrigin
public class SeatController {

    private final SeatService seatService;

    @PostMapping("/{seatId}/hold")
    @Operation(summary = "Hold a seat", description = "Hold a seat for a passenger with 120-second expiry")
    public ResponseEntity<SeatResponse> holdSeat(
            @PathVariable Long seatId,
            @Valid @RequestBody SeatHoldRequest request) {

        log.info("Hold seat request for seat {} by passenger {}", seatId, request.getPassengerId());
        SeatResponse response = seatService.holdSeat(seatId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{seatId}/confirm")
    @Operation(summary = "Confirm seat assignment", description = "Confirm a held seat assignment")
    public ResponseEntity<SeatResponse> confirmSeat(
            @PathVariable Long seatId,
            @Valid @RequestBody SeatConfirmRequest request) {

        log.info("Confirm seat request for seat {} by passenger {}", seatId, request.getPassengerId());
        SeatResponse response = seatService.confirmSeat(seatId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{seatId}/cancel")
    @Operation(summary = "Cancel seat assignment", description = "Cancel a confirmed seat assignment")
    public ResponseEntity<Void> cancelSeat(
            @PathVariable Long seatId,
            @RequestParam String passengerId) {

        log.info("Cancel seat request for seat {} by passenger {}", seatId, passengerId);
        seatService.cancelSeat(seatId, passengerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{seatId}/status")
    @Operation(summary = "Get seat status", description = "Get current status of a seat")
    public ResponseEntity<SeatResponse> getSeatStatus(@PathVariable Long seatId) {
        log.info("Get seat status request for seat {}", seatId);
        SeatResponse response = seatService.getSeatStatus(seatId);
        return ResponseEntity.ok(response);
    }
}
