package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.SeatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SeatHistory entity operations.
 */
@Repository
public interface SeatHistoryRepository extends JpaRepository<SeatHistory, Long> {

    /**
     * Find all history entries for a seat.
     *
     * @param seatId the seat ID
     * @return list of history entries
     */
    List<SeatHistory> findBySeatIdOrderByTimestampDesc(Long seatId);
}
