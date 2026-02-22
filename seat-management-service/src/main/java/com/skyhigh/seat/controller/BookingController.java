package com.skyhigh.seat.controller;

import com.skyhigh.seat.model.dto.BookingVerificationRequest;
import com.skyhigh.seat.model.dto.BookingVerificationResponse;
import com.skyhigh.seat.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for booking verification operations.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Management", description = "APIs for booking verification")
@CrossOrigin
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/verify")
    @Operation(summary = "Verify booking", description = "Verify a booking reference and passenger ID, returns flight details")
    public ResponseEntity<BookingVerificationResponse> verifyBooking(
            @Valid @RequestBody BookingVerificationRequest request) {
        
        log.info("Verify booking request for: {}", request.bookingReference());
        BookingVerificationResponse response = bookingService.verifyBooking(request);
        return ResponseEntity.ok(response);
    }
}
