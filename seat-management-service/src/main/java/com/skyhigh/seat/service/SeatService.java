package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatAlreadyHeldException;
import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.exception.SeatUnavailableException;
import com.skyhigh.seat.model.dto.SeatConfirmRequest;
import com.skyhigh.seat.model.dto.SeatHoldRequest;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.entity.SeatHistory;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.model.event.SeatConfirmedEvent;
import com.skyhigh.seat.model.event.SeatHeldEvent;
import com.skyhigh.seat.model.event.SeatReleasedEvent;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatHistoryRepository;
import com.skyhigh.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Core service for seat management operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;
    private final SeatHistoryRepository seatHistoryRepository;
    private final LockService lockService;
    private final EventPublisherService eventPublisherService;

    @Value("${seat.hold.ttl-seconds:120}")
    private int holdDurationSeconds;

    /**
     * Hold a seat for a passenger.
     */
    @Transactional
    public SeatResponse holdSeat(Long seatId, SeatHoldRequest request) {
        String lockKey = "seat:lock:" + seatId;
        RLock lock = lockService.acquireLock(lockKey);

        if (lock == null) {
            throw new SeatAlreadyHeldException("Unable to acquire lock for seat: " + seatId);
        }

        try {
            // Find seat
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new SeatNotFoundException(seatId));

            // Check if seat is already held by THIS passenger (Idempotency)
            if (seat.getStatus() == SeatStatus.HELD) {
                Optional<SeatAssignment> existingAssignment = seatAssignmentRepository
                        .findBySeatIdAndStatus(seatId, SeatAssignmentStatus.HELD);

                if (existingAssignment.isPresent()) {
                    SeatAssignment assignment = existingAssignment.get();
                    if (assignment.getPassengerId().equals(request.getPassengerId())) {
                        log.info("Seat {} already held by passenger {}. Returning existing hold.", seatId,
                                request.getPassengerId());
                        return buildSeatResponse(seat, assignment);
                    }
                }
            }

            // Check if seat is available
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatUnavailableException(seatId, seat.getStatus().toString());
            }

            // Update seat status to HELD
            seat.setStatus(SeatStatus.HELD);
            seatRepository.save(seat);

            // Create seat assignment
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusSeconds(holdDurationSeconds);

            SeatAssignment assignment = SeatAssignment.builder()
                    .seatId(seatId)
                    .passengerId(request.getPassengerId())
                    .bookingReference(request.getBookingReference())
                    .status(SeatAssignmentStatus.HELD)
                    .heldAt(now)
                    .expiresAt(expiresAt)
                    .build();

            seatAssignmentRepository.save(assignment);

            // Record history
            recordHistory(seatId, request.getPassengerId(), "HELD",
                    "Seat held until " + expiresAt);

            log.info("Seat {} held by passenger {} until {}", seatId, request.getPassengerId(), expiresAt);

            // Publish seat held event
            publishSeatHeldEvent(seat, assignment);

            return buildSeatResponse(seat, assignment);

        } finally {
            lockService.releaseLock(lock);
        }
    }

    /**
     * Confirm a seat assignment.
     */
    @Transactional
    public SeatResponse confirmSeat(Long seatId, SeatConfirmRequest request) {
        String lockKey = "seat:lock:" + seatId;
        RLock lock = lockService.acquireLock(lockKey);

        if (lock == null) {
            throw new SeatAlreadyHeldException("Unable to acquire lock for seat: " + seatId);
        }

        try {
            // Find seat
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new SeatNotFoundException(seatId));

            // Find seat assignment
            SeatAssignment assignment = seatAssignmentRepository.findBySeatIdAndStatus(
                    seatId, SeatAssignmentStatus.HELD)
                    .orElseThrow(() -> new SeatUnavailableException("No active hold found for seat: " + seatId));

            // Verify passenger
            if (!assignment.getPassengerId().equals(request.getPassengerId())) {
                throw new SeatUnavailableException("Seat is held by a different passenger");
            }

            // Check if hold has expired
            if (assignment.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new SeatUnavailableException("Seat hold has expired");
            }

            // Update seat status to CONFIRMED
            seat.setStatus(SeatStatus.CONFIRMED);
            seatRepository.save(seat);

            // Update assignment status
            assignment.setStatus(SeatAssignmentStatus.CONFIRMED);
            assignment.setConfirmedAt(LocalDateTime.now());
            seatAssignmentRepository.save(assignment);

            // Record history
            recordHistory(seatId, request.getPassengerId(), "CONFIRMED",
                    "Seat assignment confirmed");

            log.info("Seat {} confirmed for passenger {}", seatId, request.getPassengerId());

            // Publish seat confirmed event
            publishSeatConfirmedEvent(seat, assignment);

            return buildSeatResponse(seat, assignment);

        } finally {
            lockService.releaseLock(lock);
        }
    }

    /**
     * Cancel a seat assignment.
     */
    @Transactional
    public void cancelSeat(Long seatId, String passengerId) {
        String lockKey = "seat:lock:" + seatId;
        RLock lock = lockService.acquireLock(lockKey);

        if (lock == null) {
            throw new SeatAlreadyHeldException("Unable to acquire lock for seat: " + seatId);
        }

        try {
            // Find seat
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new SeatNotFoundException(seatId));

            // Find seat assignment
            Optional<SeatAssignment> assignmentOpt = seatAssignmentRepository.findBySeatId(seatId);
            if (assignmentOpt.isEmpty()) {
                throw new SeatUnavailableException("No assignment found for seat: " + seatId);
            }

            SeatAssignment assignment = assignmentOpt.get();

            // Verify passenger
            if (!assignment.getPassengerId().equals(passengerId)) {
                throw new SeatUnavailableException("Seat is assigned to a different passenger");
            }

            // Release seat
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            // Update assignment
            assignment.setStatus(SeatAssignmentStatus.CANCELLED);
            seatAssignmentRepository.save(assignment);

            // Record history
            recordHistory(seatId, passengerId, "CANCELLED", "Seat assignment cancelled");

            log.info("Seat {} cancelled by passenger {}", seatId, passengerId);

            // Publish seat released event
            publishSeatReleasedEvent(seat, passengerId, "CANCELLED");

        } finally {
            lockService.releaseLock(lock);
        }
    }

    /**
     * Get seat status.
     */
    public SeatResponse getSeatStatus(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        Optional<SeatAssignment> assignmentOpt = seatAssignmentRepository.findBySeatId(seatId);

        return buildSeatResponse(seat, assignmentOpt.orElse(null));
    }

    /**
     * Record seat history for audit trail.
     */
    private void recordHistory(Long seatId, String passengerId, String action, String details) {
        SeatHistory history = SeatHistory.builder()
                .seatId(seatId)
                .passengerId(passengerId)
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        seatHistoryRepository.save(history);
    }

    /**
     * Build seat response DTO.
     */
    private SeatResponse buildSeatResponse(Seat seat, SeatAssignment assignment) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .flightId(seat.getFlightId())
                .seatClass(seat.getSeatClass())
                .status(seat.getStatus())
                .passengerId(assignment != null ? assignment.getPassengerId() : null)
                .bookingReference(assignment != null ? assignment.getBookingReference() : null)
                .build();
    }

    /**
     * Publish seat held event.
     */
    private void publishSeatHeldEvent(Seat seat, SeatAssignment assignment) {
        try {
            SeatHeldEvent event = SeatHeldEvent.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .flightId(seat.getFlightId())
                    .flightNumber(getFlightNumber(seat.getFlightId()))
                    .passengerId(assignment.getPassengerId())
                    .bookingReference(assignment.getBookingReference())
                    .heldAt(assignment.getHeldAt())
                    .expiresAt(assignment.getExpiresAt())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherService.publishSeatHeldEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish SeatHeldEvent for seat {}: {}", seat.getId(), e.getMessage(), e);
            // Don't fail the operation if event publishing fails
        }
    }

    /**
     * Publish seat confirmed event.
     */
    private void publishSeatConfirmedEvent(Seat seat, SeatAssignment assignment) {
        try {
            SeatConfirmedEvent event = SeatConfirmedEvent.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .flightId(seat.getFlightId())
                    .flightNumber(getFlightNumber(seat.getFlightId()))
                    .passengerId(assignment.getPassengerId())
                    .bookingReference(assignment.getBookingReference())
                    .confirmedAt(assignment.getConfirmedAt())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherService.publishSeatConfirmedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish SeatConfirmedEvent for seat {}: {}", seat.getId(), e.getMessage(), e);
            // Don't fail the operation if event publishing fails
        }
    }

    /**
     * Publish seat released event.
     */
    private void publishSeatReleasedEvent(Seat seat, String passengerId, String reason) {
        try {
            SeatReleasedEvent event = SeatReleasedEvent.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .flightId(seat.getFlightId())
                    .flightNumber(getFlightNumber(seat.getFlightId()))
                    .passengerId(passengerId)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherService.publishSeatReleasedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish SeatReleasedEvent for seat {}: {}", seat.getId(), e.getMessage(), e);
            // Don't fail the operation if event publishing fails
        }
    }

    /**
     * Get flight number from flight ID.
     * In a real system, this would fetch from a Flight service.
     */
    private String getFlightNumber(Long flightId) {
        // For now, return a formatted flight number
        return "SK" + String.format("%04d", flightId);
    }
}
