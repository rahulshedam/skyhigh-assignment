package com.skyhigh.payment.controller;

import com.skyhigh.common.dto.ApiResponse;
import com.skyhigh.payment.dto.PaymentData;
import com.skyhigh.payment.dto.PaymentRequest;
import com.skyhigh.payment.service.PaymentService;
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
 * REST controller for payment operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment processing APIs")
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process a payment
     */
    @PostMapping("/process")
    @Operation(summary = "Process a payment", description = "Process a payment for excess baggage or other services. " +
            "Use simulateFailure=true to test failure scenarios.")
    public ResponseEntity<ApiResponse<PaymentData>> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("Received payment request for passenger: {}", request.getPassengerId());
        ApiResponse<PaymentData> response = paymentService.processPayment(request);

        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }

    /**
     * Get payment by reference
     */
    @GetMapping("/{paymentReference}")
    @Operation(summary = "Get payment by reference", description = "Retrieve payment details using payment reference")
    public ResponseEntity<ApiResponse<PaymentData>> getPayment(
            @PathVariable String paymentReference) {

        log.info("Fetching payment: {}", paymentReference);
        ApiResponse<PaymentData> response = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for a passenger
     */
    @GetMapping("/passenger/{passengerId}")
    @Operation(summary = "Get payments by passenger ID", description = "Retrieve all payments for a specific passenger")
    public ResponseEntity<ApiResponse<List<PaymentData>>> getPaymentsByPassenger(
            @PathVariable String passengerId) {

        log.info("Fetching payments for passenger: {}", passengerId);
        ApiResponse<List<PaymentData>> response = paymentService.getPaymentsByPassengerId(passengerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for a booking
     */
    @GetMapping("/booking/{bookingReference}")
    @Operation(summary = "Get payments by booking reference", description = "Retrieve all payments for a specific booking")
    public ResponseEntity<ApiResponse<List<PaymentData>>> getPaymentsByBooking(
            @PathVariable String bookingReference) {

        log.info("Fetching payments for booking: {}", bookingReference);
        ApiResponse<List<PaymentData>> response = paymentService.getPaymentsByBookingReference(bookingReference);
        return ResponseEntity.ok(response);
    }
}
