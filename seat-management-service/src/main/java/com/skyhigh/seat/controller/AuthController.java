package com.skyhigh.seat.controller;

import com.skyhigh.seat.model.entity.Booking;
import com.skyhigh.seat.repository.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for authentication-related operations.
 * Validates that a given email exists as a passenger in the bookings system.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for email-based authentication")
@CrossOrigin
public class AuthController {

    private final BookingRepository bookingRepository;

    /**
     * Validate that an email exists in the bookings table.
     * This is used by the frontend to verify the user before sending an OTP.
     */
    @PostMapping("/validate-email")
    @Operation(summary = "Validate passenger email",
               description = "Checks if the email belongs to a known passenger in the system")
    public ResponseEntity<Map<String, Object>> validateEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("valid", false, "message", "Email is required"));
        }

        Optional<Booking> booking = bookingRepository.findFirstByPassengerEmail(email.toLowerCase().trim());
        if (booking.isEmpty()) {
            log.info("Email validation failed - not found: {}", email);
            return ResponseEntity.ok(Map.of("valid", false, "message", "No booking found for this email"));
        }

        log.info("Email validated successfully: {}", email);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "passengerName", booking.get().getPassengerName(),
                "message", "Email verified"
        ));
    }
}
