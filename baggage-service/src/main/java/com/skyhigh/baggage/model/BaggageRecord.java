package com.skyhigh.baggage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Baggage record entity.
 */
@Entity
@Table(name = "baggage_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String baggageReference;

    @Column(nullable = false)
    private String passengerId;

    @Column(nullable = false)
    private String bookingReference;

    @Column(nullable = false)
    private BigDecimal weight;

    @Column
    private BigDecimal excessWeight;

    @Column
    private BigDecimal excessFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BaggageStatus status;

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
