package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.model.dto.SeatMapResponse;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.entity.Flight;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.repository.FlightRepository;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for seat map operations with caching.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatMapService {

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;

    /**
     * Get seat map for a flight with caching.
     * Cache TTL is configured in application.yml (30 seconds).
     */
    @Cacheable(value = "seatMaps", key = "#flightId")
    public SeatMapResponse getSeatMap(Long flightId) {
        log.debug("Fetching seat map for flight: {}", flightId);

        // Find flight
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new SeatNotFoundException("Flight not found with ID: " + flightId));

        // Find all seats for the flight
        List<Seat> seats = seatRepository.findByFlightId(flightId);

        if (seats.isEmpty()) {
            throw new SeatNotFoundException("No seats found for flight: " + flightId);
        }

        // Get all seat assignments for this flight
        List<Long> seatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
        List<SeatAssignment> assignments = seatAssignmentRepository.findAllById(seatIds).stream()
                .filter(a -> seatIds.contains(a.getSeatId()))
                .collect(Collectors.toList());

        Map<Long, SeatAssignment> assignmentMap = assignments.stream()
                .collect(Collectors.toMap(SeatAssignment::getSeatId, a -> a, (a1, a2) -> a1));

        // Build seat responses
        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> buildSeatResponse(seat, assignmentMap.get(seat.getId())))
                .collect(Collectors.toList());

        // Calculate statistics
        long availableCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long heldCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.HELD).count();
        long confirmedCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.CONFIRMED).count();

        return SeatMapResponse.builder()
                .flightId(flightId)
                .flightNumber(flight.getFlightNumber())
                .seats(seatResponses)
                .totalSeats(seats.size())
                .availableSeats((int) availableCount)
                .heldSeats((int) heldCount)
                .confirmedSeats((int) confirmedCount)
                .build();
    }

    /**
     * Build seat response DTO.
     */
    private SeatResponse buildSeatResponse(Seat seat, SeatAssignment assignment) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .flightId(seat.getFlightId())
                .seatClass(seat.getSeatClass())
                .status(seat.getStatus())
                .passengerId(assignment != null ? assignment.getPassengerId() : null)
                .bookingReference(assignment != null ? assignment.getBookingReference() : null)
                .build();
    }
}
