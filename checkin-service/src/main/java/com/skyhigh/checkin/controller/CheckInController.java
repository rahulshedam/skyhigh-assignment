package com.skyhigh.checkin.controller;

import com.skyhigh.checkin.model.dto.CheckInCompleteRequest;
import com.skyhigh.checkin.model.dto.CheckInResponse;
import com.skyhigh.checkin.model.dto.CheckInStartRequest;
import com.skyhigh.checkin.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 * REST controller for check-in operations
 */
@RestController
@RequestMapping("/api/checkin")
@Tag(name = "Check-in", description = "Check-in orchestration APIs")
@Slf4j
@CrossOrigin
public class CheckInController {

    private final CheckInService checkinService;

    public CheckInController(CheckInService checkinService) {
        this.checkinService = checkinService;
    }

    /**
     * Start check-in process
     */
    @PostMapping("/start")
    @Operation(summary = "Start check-in", description = "Initiate check-in workflow with seat hold and baggage validation")
    public ResponseEntity<CheckInResponse> startCheckIn(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody CheckInStartRequest request) {
        log.info("POST /api/checkin/start - Booking: {}", request.bookingReference());
        CheckInResponse response = checkinService.startCheckIn(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get check-in status
     */
    @GetMapping("/{checkinId}")
    @Operation(summary = "Get check-in status", description = "Retrieve current status of a check-in")
    public ResponseEntity<CheckInResponse> getCheckInStatus(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @PathVariable Long checkinId) {
        log.info("GET /api/checkin/{}", checkinId);
        CheckInResponse response = checkinService.getCheckInStatus(checkinId, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete check-in (after payment if required)
     */
    @PostMapping("/{checkinId}/complete")
    @Operation(summary = "Complete check-in", description = "Complete check-in after payment (if required)")
    public ResponseEntity<CheckInResponse> completeCheckIn(
            @PathVariable Long checkinId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestBody(required = false) CheckInCompleteRequest request) {
        log.info("POST /api/checkin/{}/complete", checkinId);
        String paymentId = request != null ? request.paymentId() : null;
        CheckInResponse response = checkinService.completeCheckIn(checkinId, paymentId, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Update baggage details
     */
    @PostMapping("/{checkinId}/baggage")
    @Operation(summary = "Update baggage", description = "Update baggage weight for an in-progress check-in")
    public ResponseEntity<CheckInResponse> updateBaggage(
            @PathVariable Long checkinId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody com.skyhigh.checkin.model.dto.CheckInBaggageRequest request) {
        log.info("POST /api/checkin/{}/baggage - Weight: {}", checkinId, request.weight());
        CheckInResponse response = checkinService.updateBaggage(checkinId, request.weight(), userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel check-in
     */
    @PostMapping("/{checkinId}/cancel")
    @Operation(summary = "Cancel check-in", description = "Cancel an in-progress check-in")
    public ResponseEntity<Void> cancelCheckIn(
            @PathVariable Long checkinId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        log.info("POST /api/checkin/{}/cancel", checkinId);
        checkinService.cancelCheckIn(checkinId, userEmail);
        return ResponseEntity.noContent().build();
    }
}
