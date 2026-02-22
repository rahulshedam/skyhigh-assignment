package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.model.dto.WaitlistJoinRequest;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.entity.SeatHistory;
import com.skyhigh.seat.model.entity.Waitlist;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.model.enums.WaitlistStatus;
import com.skyhigh.seat.model.event.WaitlistAssignedEvent;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatHistoryRepository;
import com.skyhigh.seat.repository.SeatRepository;
import com.skyhigh.seat.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for waitlist management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final SeatRepository seatRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;
    private final SeatHistoryRepository seatHistoryRepository;
    private final LockService lockService;
    private final EventPublisherService eventPublisherService;

    @Value("${seat.hold.ttl-seconds:120}")
    private int holdDurationSeconds;

    /**
     * Add a passenger to the waitlist for a seat.
     */
    @Transactional
    public Waitlist joinWaitlist(Long seatId, WaitlistJoinRequest request) {
        // Verify seat exists
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));

        // Create waitlist entry
        Waitlist waitlist = Waitlist.builder()
                .seatId(seatId)
                .passengerId(request.getPassengerId())
                .bookingReference(request.getBookingReference())
                .status(WaitlistStatus.WAITING)
                .joinedAt(LocalDateTime.now())
                .build();

        waitlistRepository.save(waitlist);

        log.info("Passenger {} joined waitlist for seat {}", request.getPassengerId(), seatId);

        return waitlist;
    }

    /**
     * Process waitlist for a specific seat.
     * Called when a seat becomes available.
     */
    @Transactional
    public void processWaitlistForSeat(Long seatId) {
        String lockKey = "seat:lock:" + seatId;
        RLock lock = lockService.acquireLock(lockKey);

        if (lock == null) {
            log.warn("Could not acquire lock for seat {} waitlist processing", seatId);
            return;
        }

        try {
            // Check if seat is available
            Seat seat = seatRepository.findById(seatId).orElse(null);
            if (seat == null || seat.getStatus() != SeatStatus.AVAILABLE) {
                log.debug("Seat {} is not available for waitlist processing", seatId);
                return;
            }

            // Get next passenger in waitlist (FIFO)
            List<Waitlist> waitingPassengers = waitlistRepository.findWaitingBySeatId(seatId);

            if (waitingPassengers.isEmpty()) {
                log.debug("No passengers waiting for seat {}", seatId);
                return;
            }

            Waitlist nextInLine = waitingPassengers.get(0);

            // Assign seat to next passenger
            assignSeatFromWaitlist(seat, nextInLine);

        } finally {
            lockService.releaseLock(lock);
        }
    }

    /**
     * Process all waitlists.
     * Called by the scheduled task every 5 seconds.
     */
    @Transactional
    public int processAllWaitlists() {
        // Find all available seats that have waiting passengers
        List<Seat> availableSeats = seatRepository.findByStatus(SeatStatus.AVAILABLE);

        int processedCount = 0;

        for (Seat seat : availableSeats) {
            try {
                List<Waitlist> waiting = waitlistRepository.findWaitingBySeatId(seat.getId());
                if (!waiting.isEmpty()) {
                    processWaitlistForSeat(seat.getId());
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process waitlist for seat {}: {}",
                        seat.getId(), e.getMessage(), e);
            }
        }

        if (processedCount > 0) {
            log.info("Processed waitlists for {} seats", processedCount);
        }

        return processedCount;
    }

    /**
     * Assign a seat to a waitlisted passenger.
     */
    private void assignSeatFromWaitlist(Seat seat, Waitlist waitlist) {
        Long seatId = seat.getId();
        String passengerId = waitlist.getPassengerId();

        // Update seat status to HELD
        seat.setStatus(SeatStatus.HELD);
        seatRepository.save(seat);

        // Create seat assignment
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(holdDurationSeconds);

        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(seatId)
                .passengerId(passengerId)
                .bookingReference(waitlist.getBookingReference())
                .status(SeatAssignmentStatus.HELD)
                .heldAt(now)
                .expiresAt(expiresAt)
                .build();

        seatAssignmentRepository.save(assignment);

        // Update waitlist status
        waitlist.setStatus(WaitlistStatus.ASSIGNED);
        waitlist.setAssignedAt(now);
        waitlistRepository.save(waitlist);

        // Record history
        SeatHistory history = SeatHistory.builder()
                .seatId(seatId)
                .passengerId(passengerId)
                .action("WAITLIST_ASSIGNED")
                .details("Seat assigned from waitlist, expires at " + expiresAt)
                .timestamp(now)
                .build();
        seatHistoryRepository.save(history);

        log.info("Assigned seat {} to passenger {} from waitlist", seatId, passengerId);

        // Publish WaitlistAssignedEvent to RabbitMQ
        publishWaitlistAssignedEvent(seat, assignment, waitlist);
    }

    /**
     * Publish waitlist assigned event.
     */
    private void publishWaitlistAssignedEvent(Seat seat, SeatAssignment assignment, Waitlist waitlist) {
        try {
            WaitlistAssignedEvent event = WaitlistAssignedEvent.builder()
                    .seatId(seat.getId())
                    .seatNumber(seat.getSeatNumber())
                    .flightId(seat.getFlightId())
                    .flightNumber(getFlightNumber(seat.getFlightId()))
                    .passengerId(assignment.getPassengerId())
                    .bookingReference(assignment.getBookingReference())
                    .waitlistId(waitlist.getId())
                    .assignedAt(assignment.getHeldAt())
                    .expiresAt(assignment.getExpiresAt())
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisherService.publishWaitlistAssignedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish WaitlistAssignedEvent for seat {}: {}", seat.getId(), e.getMessage(), e);
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

    /**
     * Get all waitlist entries for a passenger.
     */
    public List<Waitlist> getPassengerWaitlists(String passengerId) {
        return waitlistRepository.findByPassengerId(passengerId);
    }

    /**
     * Remove a passenger from waitlist.
     */
    @Transactional
    public void removeFromWaitlist(Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("Waitlist entry not found: " + waitlistId));

        waitlist.setStatus(WaitlistStatus.EXPIRED);
        waitlistRepository.save(waitlist);

        log.info("Removed passenger {} from waitlist for seat {}",
                waitlist.getPassengerId(), waitlist.getSeatId());
    }
}
