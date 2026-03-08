package com.skyhigh.seat.service;

import com.skyhigh.seat.model.entity.RateLimitAudit;
import com.skyhigh.seat.repository.RateLimitAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records rate limit / abuse events for audit and analysis.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitAuditService {

    private final RateLimitAuditRepository repository;

    @Transactional
    public void recordRateLimitExceeded(String clientIp, String requestUri) {
        try {
            repository.save(RateLimitAudit.builder()
                    .clientIp(clientIp)
                    .requestUri(requestUri != null ? (requestUri.length() > 500 ? requestUri.substring(0, 500) : requestUri) : null)
                    .action("RATE_LIMIT_EXCEEDED")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist rate limit audit record: {}", e.getMessage());
        }
    }
}
