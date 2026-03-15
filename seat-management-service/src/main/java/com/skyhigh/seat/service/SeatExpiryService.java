package com.skyhigh.seat.service;

import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.entity.SeatHistory;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.model.event.SeatReleasedEvent;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatHistoryRepository;
import com.skyhigh.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.redisson.api.RLock;

/**
 * Service for releasing expired seat holds.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatExpiryService {

    private final SeatRepository seatRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;
    private final SeatHistoryRepository seatHistoryRepository;
    private final WaitlistService waitlistService;
    private final EventPublisherService eventPublisherService;
    private final LockService lockService;

    /**
     * Release all expired seat holds.
     * Called by the scheduled task every 10 seconds.
     */
    @Transactional
    public int releaseExpiredSeats() {
        LocalDateTime now = LocalDateTime.now();

        // Find all held seats that have expired
        List<SeatAssignment> expiredAssignments = seatAssignmentRepository
                .findByExpiresAtBeforeAndStatus(now, SeatAssignmentStatus.HELD);

        if (expiredAssignments.isEmpty()) {
            log.debug("No expired seat holds found");
            return 0;
        }

        log.info("Found {} expired seat holds to release", expiredAssignments.size());

        int releasedCount = 0;

        for (SeatAssignment assignment : expiredAssignments) {
            try {
                releaseSeat(assignment);
                releasedCount++;
            } catch (Exception e) {
                log.error("Failed to release expired seat assignment {}: {}",
                        assignment.getId(), e.getMessage(), e);
            }
        }

        log.info("Released {} expired seat holds", releasedCount);
        return releasedCount;
    }

    /**
     * Release a single expired seat.
     */
    private void releaseSeat(SeatAssignment assignment) {
        Long seatId = assignment.getSeatId();
        String lockKey = "seat:lock:" + seatId;

        RLock lock = lockService.acquireLock(lockKey);
        if (lock == null) {
            log.warn("Skipping expiry for seat {} - could not acquire lock", seatId);
            return;
        }

        try {
            // Re-fetch the latest assignment state inside the lock to avoid races
            SeatAssignment latestAssignment = seatAssignmentRepository.findById(assignment.getId())
                    .orElse(null);
            if (latestAssignment == null) {
                log.warn("Skipping expiry for seat {} - assignment {} no longer exists", seatId, assignment.getId());
                return;
            }

            // If the assignment is no longer HELD, another flow (confirm/cancel) already won
            if (latestAssignment.getStatus() != SeatAssignmentStatus.HELD) {
                log.debug("Skipping expiry for seat {} - assignment {} is now {}", seatId,
                        latestAssignment.getId(), latestAssignment.getStatus());
                return;
            }

            // Double-check that the hold is still expired (authoritative check)
            LocalDateTime now = LocalDateTime.now();
            if (latestAssignment.getExpiresAt() == null || !latestAssignment.getExpiresAt().isBefore(now)) {
                log.debug("Skipping expiry for seat {} - hold not expired anymore (expiresAt={})",
                        seatId, latestAssignment.getExpiresAt());
                return;
            }

            // Update seat status to AVAILABLE, but never override a confirmed seat defensively
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

            if (seat.getStatus() == SeatStatus.CONFIRMED) {
                log.info("Skipping expiry for seat {} - seat already CONFIRMED", seatId);
                return;
            }

            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            // Update assignment status to CANCELLED
            latestAssignment.setStatus(SeatAssignmentStatus.CANCELLED);
            seatAssignmentRepository.save(latestAssignment);

            // Record history
            SeatHistory history = SeatHistory.builder()
                    .seatId(seatId)
                    .passengerId(latestAssignment.getPassengerId())
                    .action("EXPIRED")
                    .details("Seat hold expired after 120 seconds")
                    .timestamp(LocalDateTime.now())
                    .build();
            seatHistoryRepository.save(history);

            log.info("Released expired seat {} held by passenger {}",
                    seatId, latestAssignment.getPassengerId());

            // Publish seat released event
            publishSeatReleasedEvent(seat, latestAssignment.getPassengerId(), "EXPIRED");

            // Process waitlist for this seat
            try {
                waitlistService.processWaitlistForSeat(seatId);
            } catch (Exception e) {
                log.error("Failed to process waitlist for seat {}: {}", seatId, e.getMessage(), e);
            }
        } finally {
            lockService.releaseLock(lock);
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
