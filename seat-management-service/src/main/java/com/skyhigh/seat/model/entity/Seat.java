package com.skyhigh.seat.model.entity;

import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a seat on a flight.
 * Uses optimistic locking via @Version for concurrency control.
 */
@Entity
@Table(name = "seats", indexes = {
        @Index(name = "idx_flight_id", columnList = "flight_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_flight_status", columnList = "flight_id,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String seatNumber;

    @Column(nullable = false, name = "flight_id")
    private Long flightId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    /**
     * Version field for optimistic locking to prevent concurrent modifications
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SeatStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
