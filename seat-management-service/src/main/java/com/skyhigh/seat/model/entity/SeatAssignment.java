package com.skyhigh.seat.model.entity;

import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a seat assignment to a passenger.
 * Tracks hold and confirmation status with expiry times.
 */
@Entity
@Table(name = "seat_assignments", indexes = {
        @Index(name = "idx_seat_id", columnList = "seat_id"),
        @Index(name = "idx_passenger_id", columnList = "passenger_id"),
        @Index(name = "idx_expires_at_status", columnList = "expires_at,status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_active", columnNames = { "seat_id", "status" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAssignment {

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
    private SeatAssignmentStatus status;

    /**
     * Timestamp when the seat was held
     */
    @Column(name = "held_at")
    private LocalDateTime heldAt;

    /**
     * Timestamp when the hold expires (heldAt + 120 seconds)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Timestamp when the seat was confirmed
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
