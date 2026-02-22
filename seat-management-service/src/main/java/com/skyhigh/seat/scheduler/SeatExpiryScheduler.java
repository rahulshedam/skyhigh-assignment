package com.skyhigh.seat.scheduler;

import com.skyhigh.seat.service.SeatExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to release expired seat holds.
 * Runs every 10 seconds.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SeatExpiryScheduler {

    private final SeatExpiryService seatExpiryService;

    /**
     * Release expired seat holds every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void releaseExpiredSeats() {
        log.debug("Running seat expiry job...");

        try {
            int releasedCount = seatExpiryService.releaseExpiredSeats();

            if (releasedCount > 0) {
                log.info("Seat expiry job completed: {} seats released", releasedCount);
            }
        } catch (Exception e) {
            log.error("Error in seat expiry job: {}", e.getMessage(), e);
        }
    }
}
