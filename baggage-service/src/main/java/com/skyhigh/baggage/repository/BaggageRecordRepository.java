package com.skyhigh.baggage.repository;

import com.skyhigh.baggage.model.BaggageRecord;
import com.skyhigh.baggage.model.BaggageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for baggage records.
 */
@Repository
public interface BaggageRecordRepository extends JpaRepository<BaggageRecord, Long> {

    Optional<BaggageRecord> findByBaggageReference(String baggageReference);

    List<BaggageRecord> findByPassengerId(String passengerId);

    List<BaggageRecord> findByBookingReference(String bookingReference);

    List<BaggageRecord> findByStatus(BaggageStatus status);
}
