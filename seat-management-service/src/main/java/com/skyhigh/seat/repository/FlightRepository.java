package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Flight entity operations.
 */
@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * Find flight by flight number.
     *
     * @param flightNumber the flight number
     * @return Optional containing the flight if found
     */
    Optional<Flight> findByFlightNumber(String flightNumber);
}
