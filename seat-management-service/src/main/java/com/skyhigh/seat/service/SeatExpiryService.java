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

        // Update seat status to AVAILABLE
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

        seat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);

        // Update assignment status to CANCELLED
        assignment.setStatus(SeatAssignmentStatus.CANCELLED);
        seatAssignmentRepository.save(assignment);

        // Record history
        SeatHistory history = SeatHistory.builder()
                .seatId(seatId)
                .passengerId(assignment.getPassengerId())
                .action("EXPIRED")
                .details("Seat hold expired after 120 seconds")
                .timestamp(LocalDateTime.now())
                .build();
        seatHistoryRepository.save(history);

        log.info("Released expired seat {} held by passenger {}",
                seatId, assignment.getPassengerId());

        // Publish seat released event
        publishSeatReleasedEvent(seat, assignment.getPassengerId(), "EXPIRED");

        // Process waitlist for this seat
        try {
            waitlistService.processWaitlistForSeat(seatId);
        } catch (Exception e) {
            log.error("Failed to process waitlist for seat {}: {}", seatId, e.getMessage(), e);
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
