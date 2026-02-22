package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SeatAssignment entity operations.
 */
@Repository
public interface SeatAssignmentRepository extends JpaRepository<SeatAssignment, Long> {

    /**
     * Find seat assignment by seat ID.
     *
     * @param seatId the seat ID
     * @return Optional containing the seat assignment if found
     */
    Optional<SeatAssignment> findBySeatId(Long seatId);

    /**
     * Find all seat assignments for a passenger.
     *
     * @param passengerId the passenger ID
     * @return list of seat assignments
     */
    List<SeatAssignment> findByPassengerId(String passengerId);

    /**
     * Find all seat assignments that have expired.
     * Used by the expiry scheduler to release held seats.
     *
     * @param time   the current time
     * @param status the assignment status (typically HELD)
     * @return list of expired seat assignments
     */
    List<SeatAssignment> findByExpiresAtBeforeAndStatus(LocalDateTime time, SeatAssignmentStatus status);

    /**
     * Find seat assignment by seat ID and status.
     *
     * @param seatId the seat ID
     * @param status the assignment status
     * @return Optional containing the seat assignment if found
     */
    Optional<SeatAssignment> findBySeatIdAndStatus(Long seatId, SeatAssignmentStatus status);
}
