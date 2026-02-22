package com.skyhigh.checkin.service;

import com.skyhigh.checkin.client.BaggageServiceClient;
import com.skyhigh.checkin.client.PaymentServiceClient;
import com.skyhigh.checkin.client.SeatManagementClient;
import com.skyhigh.checkin.client.dto.*;
import com.skyhigh.checkin.exception.CheckInException;
import com.skyhigh.checkin.exception.CheckInNotFoundException;
import com.skyhigh.checkin.model.dto.CheckInCompleteRequest;
import com.skyhigh.checkin.model.dto.CheckInResponse;
import com.skyhigh.checkin.model.dto.CheckInStartRequest;
import com.skyhigh.checkin.model.entity.CheckIn;
import com.skyhigh.checkin.model.entity.CheckInHistory;
import com.skyhigh.checkin.model.enums.CheckInStatus;
import com.skyhigh.checkin.repository.CheckInHistoryRepository;
import com.skyhigh.checkin.repository.CheckInRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Core check-in orchestration service
 */
@Service
@Transactional
@Slf4j
public class CheckInService {

    private final CheckInRepository checkinRepository;
    private final CheckInHistoryRepository historyRepository;
    private final SeatManagementClient seatClient;
    private final BaggageServiceClient baggageClient;
    private final PaymentServiceClient paymentClient;

    public CheckInService(
            CheckInRepository checkinRepository,
            CheckInHistoryRepository historyRepository,
            SeatManagementClient seatClient,
            BaggageServiceClient baggageClient,
            PaymentServiceClient paymentClient) {
        this.checkinRepository = checkinRepository;
        this.historyRepository = historyRepository;
        this.seatClient = seatClient;
        this.baggageClient = baggageClient;
        this.paymentClient = paymentClient;
    }

    /**
     * Start check-in workflow
     * 1. Create check-in record
     * 2. Hold seat via Seat Management Service
     * 3. Validate baggage via Baggage Service
     * 4. If baggage > 25kg: pause for payment
     * 5. Else: complete check-in immediately
     */
    public CheckInResponse startCheckIn(CheckInStartRequest request) {
        log.info("Starting check-in for booking: {}, passenger: {}",
                request.bookingReference(), request.passengerId());

        // 1. Create check-in record
        Double baggageWeight = request.baggageWeight() != null ? request.baggageWeight() : 0.0;

        CheckIn checkIn = CheckIn.builder()
                .bookingReference(request.bookingReference())
                .passengerId(request.passengerId())
                .flightId(request.flightId())
                .seatId(request.seatId())
                .status(CheckInStatus.IN_PROGRESS)
                .baggageWeight(baggageWeight)
                .build();

        checkIn = checkinRepository.save(checkIn);
        recordHistory(checkIn, CheckInStatus.IN_PROGRESS, "CHECK_IN_STARTED",
                "Check-in process initiated");

        try {
            // 2. Hold seat
            log.info("Holding seat {} for check-in {}", request.seatId(), checkIn.getId());
            SeatResponse seat = seatClient.holdSeat(
                    request.seatId(),
                    new SeatHoldRequest(request.passengerId(), request.bookingReference()));

            // 3. Validate baggage (only if provided)
            if (baggageWeight > 0) {
                log.info("Validating baggage weight: {} kg", baggageWeight);
                BaggageValidationResponse baggage = baggageClient.validateBaggage(
                        request.passengerId(),
                        request.bookingReference(),
                        baggageWeight
                );

                // 4. Check if payment required
                if (baggage.requiresPayment()) {
                    log.info("Excess baggage detected. Fee: ${}", baggage.excessFeeAsDouble());
                    checkIn.setStatus(CheckInStatus.WAITING_FOR_PAYMENT);
                    checkIn.setExcessBaggageFee(baggage.excessFeeAsDouble());
                    checkIn = checkinRepository.save(checkIn);

                    recordHistory(checkIn, CheckInStatus.WAITING_FOR_PAYMENT, "PAYMENT_REQUIRED",
                            String.format("Excess baggage fee: $%.2f", baggage.excessFeeAsDouble()));

                    return toResponse(checkIn,
                            String.format("Payment required for excess baggage. Fee: $%.2f", baggage.excessFeeAsDouble()));
                }

                // 5. Complete check-in immediately (no payment needed)
                log.info("No excess baggage from start request. Completing check-in immediately.");
                return completeCheckIn(checkIn.getId(), null);
            }

            // If baggageWeight == 0, stay in IN_PROGRESS
            log.info("Check-in started with no baggage. Waiting for baggage details.");
            return toResponse(checkIn, "Check-in started. Waiting for baggage details.");

        } catch (Exception e) {
            log.error("Check-in failed: {}", e.getMessage(), e);

            // Rollback: cancel seat hold
            try {
                seatClient.cancelSeat(request.seatId(), request.passengerId());
            } catch (Exception cancelEx) {
                log.error("Failed to cancel seat during rollback: {}", cancelEx.getMessage());
            }

            checkIn.setStatus(CheckInStatus.CANCELLED);
            checkinRepository.save(checkIn);
            recordHistory(checkIn, CheckInStatus.CANCELLED, "CHECK_IN_FAILED",
                    "Check-in failed: " + e.getMessage());

            throw new CheckInException("Check-in failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update baggage weight for existing check-in
     */
    public CheckInResponse updateBaggage(Long checkinId, Double weight) {
        log.info("Updating baggage for check-in {}: {} kg", checkinId, weight);

        CheckIn checkIn = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new CheckInNotFoundException(checkinId));

        if (checkIn.getStatus() == CheckInStatus.COMPLETED || checkIn.getStatus() == CheckInStatus.CANCELLED) {
            throw new CheckInException("Cannot update baggage for finalized check-in");
        }

        checkIn.setBaggageWeight(weight);

        if (weight > 0) {
            BaggageValidationResponse baggage = baggageClient.validateBaggage(
                    checkIn.getPassengerId(),
                    checkIn.getBookingReference(),
                    weight
            );
            if (baggage.requiresPayment()) {
                checkIn.setStatus(CheckInStatus.WAITING_FOR_PAYMENT);
                checkIn.setExcessBaggageFee(baggage.excessFeeAsDouble());
                checkIn = checkinRepository.save(checkIn);

                recordHistory(checkIn, CheckInStatus.WAITING_FOR_PAYMENT, "BAGGAGE_UPDATED",
                        String.format("Updated baggage: %.1fkg. Excess fee: $%.2f", weight, baggage.excessFeeAsDouble()));

                return toResponse(checkIn, "Payment required for excess baggage");
            }
        } else {
            // Reset fee if weight is set to 0
            checkIn.setExcessBaggageFee(0.0);
        }

        // No payment required
        log.info("No excess baggage. Completing check-in.");
        return completeCheckIn(checkinId, null);
    }

    /**
     * Complete check-in (after payment if required)
     */
    public CheckInResponse completeCheckIn(Long checkinId, String paymentId) {
        log.info("Completing check-in {}", checkinId);

        CheckIn checkIn = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new CheckInNotFoundException(checkinId));

        // Validate state
        if (checkIn.getStatus() == CheckInStatus.COMPLETED) {
            throw new CheckInException("Check-in already completed");
        }

        if (checkIn.getStatus() == CheckInStatus.CANCELLED) {
            throw new CheckInException("Check-in was cancelled");
        }

        // If payment was required, verify it's provided
        if (checkIn.getExcessBaggageFee() != null && checkIn.getExcessBaggageFee() > 0) {
            if (paymentId == null || paymentId.isBlank()) {
                throw new CheckInException("Payment required but not provided");
            }
            checkIn.setPaymentId(paymentId);
            log.info("Payment {} recorded for check-in {}", paymentId, checkinId);
        }

        // Confirm seat
        log.info("Confirming seat {} for check-in {}", checkIn.getSeatId(), checkinId);
        seatClient.confirmSeat(
                checkIn.getSeatId(),
                new SeatConfirmRequest(checkIn.getPassengerId(), checkIn.getBookingReference()));

        // Update status
        checkIn.setStatus(CheckInStatus.COMPLETED);
        checkIn.setCompletedAt(LocalDateTime.now());
        checkIn = checkinRepository.save(checkIn);

        recordHistory(checkIn, CheckInStatus.COMPLETED, "CHECK_IN_COMPLETED",
                "Check-in completed successfully");

        log.info("Check-in {} completed successfully", checkinId);
        return toResponse(checkIn, "Check-in completed successfully");
    }

    /**
     * Cancel check-in
     */
    public void cancelCheckIn(Long checkinId) {
        log.info("Cancelling check-in {}", checkinId);

        CheckIn checkIn = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new CheckInNotFoundException(checkinId));

        if (checkIn.getStatus() == CheckInStatus.COMPLETED) {
            throw new CheckInException("Cannot cancel completed check-in");
        }

        if (checkIn.getStatus() == CheckInStatus.CANCELLED) {
            throw new CheckInException("Check-in already cancelled");
        }

        // Cancel seat hold
        seatClient.cancelSeat(checkIn.getSeatId(), checkIn.getPassengerId());

        checkIn.setStatus(CheckInStatus.CANCELLED);
        checkinRepository.save(checkIn);

        recordHistory(checkIn, CheckInStatus.CANCELLED, "CHECK_IN_CANCELLED",
                "Check-in cancelled by user");

        log.info("Check-in {} cancelled successfully", checkinId);
    }

    /**
     * Get check-in status
     */
    @Transactional(readOnly = true)
    public CheckInResponse getCheckInStatus(Long checkinId) {
        CheckIn checkIn = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new CheckInNotFoundException(checkinId));

        return toResponse(checkIn, null);
    }

    /**
     * Record state transition in history
     */
    private void recordHistory(CheckIn checkIn, CheckInStatus toStatus, String action, String details) {
        CheckInHistory history = CheckInHistory.builder()
                .checkinId(checkIn.getId())
                .fromStatus(checkIn.getStatus())
                .toStatus(toStatus)
                .action(action)
                .details(details)
                .build();

        historyRepository.save(history);
    }

    /**
     * Convert entity to response DTO
     */
    private CheckInResponse toResponse(CheckIn checkIn, String message) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getBookingReference(),
                checkIn.getPassengerId(),
                checkIn.getFlightId(),
                checkIn.getSeatId(),
                checkIn.getStatus(),
                checkIn.getBaggageWeight(),
                checkIn.getExcessBaggageFee(),
                checkIn.getPaymentId(),
                message,
                checkIn.getCreatedAt(),
                checkIn.getCompletedAt());
    }
}
