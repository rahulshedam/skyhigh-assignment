package com.skyhigh.seat.scheduler;

import com.skyhigh.seat.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to process waitlists.
 * Runs every 5 seconds.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WaitlistProcessorScheduler {

    private final WaitlistService waitlistService;

    /**
     * Process waitlists every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000) // 5 seconds
    public void processWaitlists() {
        log.debug("Running waitlist processor job...");

        try {
            int processedCount = waitlistService.processAllWaitlists();

            if (processedCount > 0) {
                log.info("Waitlist processor job completed: {} seats assigned", processedCount);
            }
        } catch (Exception e) {
            log.error("Error in waitlist processor job: {}", e.getMessage(), e);
        }
    }
}
