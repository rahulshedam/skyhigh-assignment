package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.model.dto.SeatMapResponse;
import com.skyhigh.seat.model.entity.Flight;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.repository.FlightRepository;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatMapServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private SeatAssignmentRepository seatAssignmentRepository;

    @InjectMocks
    private SeatMapService seatMapService;

    private Flight testFlight;
    private List<Seat> testSeats;

    @BeforeEach
    void setUp() {
        testFlight = new Flight();
        testFlight.setId(1L);
        testFlight.setFlightNumber("SK101");
        testFlight.setDepartureTime(LocalDateTime.now().plusHours(2));
        testFlight.setArrivalTime(LocalDateTime.now().plusHours(5));

        Seat seat1 = new Seat();
        seat1.setId(1L);
        seat1.setSeatNumber("1A");
        seat1.setFlightId(1L);
        seat1.setSeatClass(SeatClass.ECONOMY);
        seat1.setStatus(SeatStatus.AVAILABLE);

        Seat seat2 = new Seat();
        seat2.setId(2L);
        seat2.setSeatNumber("1B");
        seat2.setFlightId(1L);
        seat2.setSeatClass(SeatClass.ECONOMY);
        seat2.setStatus(SeatStatus.HELD);

        Seat seat3 = new Seat();
        seat3.setId(3L);
        seat3.setSeatNumber("1C");
        seat3.setFlightId(1L);
        seat3.setSeatClass(SeatClass.ECONOMY);
        seat3.setStatus(SeatStatus.CONFIRMED);

        testSeats = Arrays.asList(seat1, seat2, seat3);
    }

    @Test
    void getSeatMap_Success() {
        // Arrange
        when(flightRepository.findById(1L)).thenReturn(Optional.of(testFlight));
        when(seatRepository.findByFlightId(1L)).thenReturn(testSeats);
        when(seatAssignmentRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        // Act
        SeatMapResponse response = seatMapService.getSeatMap(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getFlightId());
        assertEquals("SK101", response.getFlightNumber());
        assertEquals(3, response.getTotalSeats());
        assertEquals(1, response.getAvailableSeats());
        assertEquals(1, response.getHeldSeats());
        assertEquals(1, response.getConfirmedSeats());
        assertEquals(3, response.getSeats().size());
    }

    @Test
    void getSeatMap_FlightNotFound_ThrowsException() {
        // Arrange
        when(flightRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> seatMapService.getSeatMap(1L));
    }

    @Test
    void getSeatMap_NoSeats_ThrowsException() {
        // Arrange
        when(flightRepository.findById(1L)).thenReturn(Optional.of(testFlight));
        when(seatRepository.findByFlightId(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> seatMapService.getSeatMap(1L));
    }

    @Test
    void getSeatMap_WithAssignments() {
        // Arrange
        SeatAssignment assignment = SeatAssignment.builder()
                .id(1L)
                .seatId(2L)
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .build();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(testFlight));
        when(seatRepository.findByFlightId(1L)).thenReturn(testSeats);
        when(seatAssignmentRepository.findAllById(anyList())).thenReturn(Collections.singletonList(assignment));

        // Act
        SeatMapResponse response = seatMapService.getSeatMap(1L);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getSeats().size());

        // Find seat 2 (1B) which should have assignment
        var seat2Response = response.getSeats().stream()
                .filter(s -> s.getId().equals(2L))
                .findFirst();

        assertTrue(seat2Response.isPresent());
        assertEquals("PASS123", seat2Response.get().getPassengerId());
        assertEquals("BOOK456", seat2Response.get().getBookingReference());
    }
}
