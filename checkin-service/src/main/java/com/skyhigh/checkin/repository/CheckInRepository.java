package com.skyhigh.checkin.repository;

import com.skyhigh.checkin.model.entity.CheckIn;
import com.skyhigh.checkin.model.enums.CheckInStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CheckIn entity
 */
@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /**
     * Find check-in by booking reference and passenger ID
     */
    Optional<CheckIn> findByBookingReferenceAndPassengerId(String bookingReference, String passengerId);

    /**
     * Find all check-ins by passenger ID
     */
    List<CheckIn> findByPassengerId(String passengerId);

    /**
     * Find all check-ins by status
     */
    List<CheckIn> findByStatus(CheckInStatus status);

    /**
     * Find all check-ins for a flight
     */
    List<CheckIn> findByFlightId(Long flightId);
}
