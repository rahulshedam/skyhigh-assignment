package com.skyhigh.seat.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for audit trail of rate limit / abuse detection events.
 */
@Entity
@Table(name = "rate_limit_audit", indexes = {
        @Index(name = "idx_rate_limit_audit_ip", columnList = "client_ip"),
        @Index(name = "idx_rate_limit_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45, name = "client_ip")
    private String clientIp;

    @Column(length = 500, name = "request_uri")
    private String requestUri;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String action = "RATE_LIMIT_EXCEEDED";

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
