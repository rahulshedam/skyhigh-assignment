package com.skyhigh.seat.repository;

import com.skyhigh.seat.model.entity.Waitlist;
import com.skyhigh.seat.model.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Waitlist entity operations.
 */
@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    /**
     * Find all waitlist entries for a seat with a specific status, ordered by join
     * time (FIFO).
     *
     * @param seatId the seat ID
     * @param status the waitlist status
     * @return list of waitlist entries ordered by joinedAt ascending
     */
    List<Waitlist> findBySeatIdAndStatusOrderByJoinedAtAsc(Long seatId, WaitlistStatus status);

    /**
     * Find all waitlist entries for a passenger.
     *
     * @param passengerId the passenger ID
     * @return list of waitlist entries
     */
    List<Waitlist> findByPassengerId(String passengerId);

    /**
     * Find all waiting entries for a seat (convenience method).
     *
     * @param seatId the seat ID
     * @return list of waiting entries ordered by join time
     */
    default List<Waitlist> findWaitingBySeatId(Long seatId) {
        return findBySeatIdAndStatusOrderByJoinedAtAsc(seatId, WaitlistStatus.WAITING);
    }
}
