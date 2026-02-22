package com.skyhigh.seat.controller;

import com.skyhigh.seat.model.dto.WaitlistJoinRequest;
import com.skyhigh.seat.model.entity.Waitlist;
import com.skyhigh.seat.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for waitlist operations.
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Waitlist Management", description = "APIs for waitlist operations")
@CrossOrigin
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/{seatId}/waitlist")
    @Operation(summary = "Join waitlist", description = "Add passenger to waitlist for a seat")
    public ResponseEntity<Waitlist> joinWaitlist(
            @PathVariable Long seatId,
            @Valid @RequestBody WaitlistJoinRequest request) {

        log.info("Join waitlist request for seat {} by passenger {}",
                seatId, request.getPassengerId());

        Waitlist waitlist = waitlistService.joinWaitlist(seatId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlist);
    }

    @GetMapping("/waitlist/{passengerId}")
    @Operation(summary = "Get passenger waitlists", description = "Get all waitlist entries for a passenger")
    public ResponseEntity<List<Waitlist>> getPassengerWaitlists(@PathVariable String passengerId) {
        log.info("Get waitlist request for passenger {}", passengerId);

        List<Waitlist> waitlists = waitlistService.getPassengerWaitlists(passengerId);
        return ResponseEntity.ok(waitlists);
    }

    @DeleteMapping("/waitlist/{waitlistId}")
    @Operation(summary = "Remove from waitlist", description = "Remove a passenger from waitlist")
    public ResponseEntity<Void> removeFromWaitlist(@PathVariable Long waitlistId) {
        log.info("Remove from waitlist request for waitlist ID {}", waitlistId);

        waitlistService.removeFromWaitlist(waitlistId);
        return ResponseEntity.noContent().build();
    }
}
