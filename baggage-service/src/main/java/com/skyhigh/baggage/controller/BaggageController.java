package com.skyhigh.baggage.controller;

import com.skyhigh.baggage.dto.BaggageData;
import com.skyhigh.baggage.dto.BaggageValidationRequest;
import com.skyhigh.baggage.dto.BaggageValidationResponse;
import com.skyhigh.baggage.service.BaggageService;
import com.skyhigh.common.dto.ApiResponse;
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
 * REST controller for baggage operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/baggage")
@RequiredArgsConstructor
@Tag(name = "Baggage", description = "Baggage validation and fee calculation APIs")
@CrossOrigin
public class BaggageController {

    private final BaggageService baggageService;

    /**
     * Validate baggage weight and calculate fees.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate baggage", description = "Validate baggage weight and calculate excess fees if applicable")
    public ResponseEntity<ApiResponse<BaggageValidationResponse>> validateBaggage(
            @Valid @RequestBody BaggageValidationRequest request) {

        log.info("Received baggage validation request for passenger: {}", request.getPassengerId());
        ApiResponse<BaggageValidationResponse> response = baggageService.validateBaggage(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get baggage by reference.
     */
    @GetMapping("/{baggageReference}")
    @Operation(summary = "Get baggage by reference", description = "Retrieve baggage details using baggage reference")
    public ResponseEntity<ApiResponse<BaggageData>> getBaggage(
            @PathVariable String baggageReference) {

        log.info("Fetching baggage: {}", baggageReference);
        ApiResponse<BaggageData> response = baggageService.getBaggageByReference(baggageReference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all baggage for a passenger.
     */
    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get baggage by passenger ID", description = "Retrieve all baggage records for a specific passenger")
    public ResponseEntity<ApiResponse<List<BaggageData>>> getBaggageByPassenger(
            @PathVariable String passengerId) {

        log.info("Fetching baggage for passenger: {}", passengerId);
        ApiResponse<List<BaggageData>> response = baggageService.getBaggageByPassengerId(passengerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all baggage for a booking.
     */
    @GetMapping("/booking/{bookingReference}")
    @Operation(summary = "Get baggage by booking reference", description = "Retrieve all baggage records for a specific booking")
    public ResponseEntity<ApiResponse<List<BaggageData>>> getBaggageByBooking(
            @PathVariable String bookingReference) {

        log.info("Fetching baggage for booking: {}", bookingReference);
        ApiResponse<List<BaggageData>> response = baggageService.getBaggageByBookingReference(bookingReference);
        return ResponseEntity.ok(response);
    }
}
