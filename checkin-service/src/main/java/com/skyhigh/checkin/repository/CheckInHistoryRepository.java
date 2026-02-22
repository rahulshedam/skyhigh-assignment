package com.skyhigh.checkin.repository;

import com.skyhigh.checkin.model.entity.CheckInHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CheckInHistory entity
 */
@Repository
public interface CheckInHistoryRepository extends JpaRepository<CheckInHistory, Long> {

    /**
     * Find all history records for a check-in
     */
    List<CheckInHistory> findByCheckinIdOrderByTimestampDesc(Long checkinId);
}
