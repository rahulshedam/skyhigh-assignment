package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.RateLimitAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RateLimitAuditRepository extends JpaRepository<RateLimitAudit, Long> {

    List<RateLimitAudit> findByClientIpOrderByCreatedAtDesc(String clientIp, Pageable pageable);
}
