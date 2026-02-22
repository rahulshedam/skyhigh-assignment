package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    Optional<Booking> findByBookingReferenceAndPassengerId(String bookingReference, String passengerId);
    
    boolean existsByBookingReferenceAndPassengerId(String bookingReference, String passengerId);
}
