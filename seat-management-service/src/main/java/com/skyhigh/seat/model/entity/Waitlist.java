package com.skyhigh.seat.model.entity;

import com.skyhigh.seat.model.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a waitlist entry for a seat.
 * Implements FIFO queue ordering via joinedAt timestamp.
 */
@Entity
@Table(name = "waitlist", indexes = {
        @Index(name = "idx_seat_status_joined", columnList = "seat_id,status,joined_at"),
        @Index(name = "idx_passenger_id", columnList = "passenger_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "seat_id")
    private Long seatId;

    @Column(nullable = false, length = 50, name = "passenger_id")
    private String passengerId;

    @Column(nullable = false, length = 50)
    private String bookingReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status;

    /**
     * Timestamp when passenger joined the waitlist (used for FIFO ordering)
     */
    @Column(nullable = false, name = "joined_at")
    private LocalDateTime joinedAt;

    /**
     * Timestamp when seat was assigned to this waitlisted passenger
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = WaitlistStatus.WAITING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
