package com.skyhigh.checkin.model.entity;

import com.skyhigh.checkin.model.enums.CheckInStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Audit trail for check-in state transitions
 */
@Entity
@Table(name = "checkin_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CheckInHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long checkinId;

    @Enumerated(EnumType.STRING)
    @Column
    private CheckInStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInStatus toStatus;

    @Column(nullable = false)
    private String action;

    @Column(length = 1000)
    private String details;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
