package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Seat entity operations.
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Find all seats for a given flight.
     *
     * @param flightId the flight ID
     * @return list of seats
     */
    List<Seat> findByFlightId(Long flightId);

    /**
     * Find seat by ID and status.
     *
     * @param id     the seat ID
     * @param status the seat status
     * @return Optional containing the seat if found
     */
    Optional<Seat> findByIdAndStatus(Long id, SeatStatus status);

    /**
     * Find all seats for a flight with a specific status.
     *
     * @param flightId the flight ID
     * @param status   the seat status
     * @return list of seats
     */
    List<Seat> findByFlightIdAndStatus(Long flightId, SeatStatus status);

    /**
     * Find all seats with a specific status.
     *
     * @param status the seat status
     * @return list of seats
     */
    List<Seat> findByStatus(SeatStatus status);
}
