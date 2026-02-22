package com.skyhigh.seat.service;

import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatHistoryRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatExpiryServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatAssignmentRepository seatAssignmentRepository;

    @Mock
    private SeatHistoryRepository seatHistoryRepository;

    @Mock
    private WaitlistService waitlistService;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private SeatExpiryService seatExpiryService;

    private SeatAssignment expiredAssignment;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("1A");
        testSeat.setStatus(SeatStatus.HELD);

        expiredAssignment = SeatAssignment.builder()
                .id(1L)
                .seatId(1L)
                .passengerId("PASS123")
                .status(SeatAssignmentStatus.HELD)
                .heldAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().minusSeconds(10))
                .build();
    }

    @Test
    void releaseExpiredSeats_NoExpiredSeats() {
        // Arrange
        when(seatAssignmentRepository.findByExpiresAtBeforeAndStatus(any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        int result = seatExpiryService.releaseExpiredSeats();

        // Assert
        assertEquals(0, result);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void releaseExpiredSeats_Success() {
        // Arrange
        when(seatAssignmentRepository.findByExpiresAtBeforeAndStatus(any(), any()))
                .thenReturn(Collections.singletonList(expiredAssignment));
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any(SeatAssignment.class))).thenReturn(expiredAssignment);
        doNothing().when(waitlistService).processWaitlistForSeat(1L);

        // Act
        int result = seatExpiryService.releaseExpiredSeats();

        // Assert
        assertEquals(1, result);
        verify(seatRepository).save(any(Seat.class));
        verify(seatAssignmentRepository).save(any(SeatAssignment.class));
        verify(seatHistoryRepository).save(any());
        verify(waitlistService).processWaitlistForSeat(1L);
    }

    @Test
    void releaseExpiredSeats_MultipleSeats() {
        // Arrange
        SeatAssignment expiredAssignment2 = SeatAssignment.builder()
                .id(2L)
                .seatId(2L)
                .passengerId("PASS456")
                .status(SeatAssignmentStatus.HELD)
                .expiresAt(LocalDateTime.now().minusSeconds(5))
                .build();

        Seat testSeat2 = new Seat();
        testSeat2.setId(2L);
        testSeat2.setStatus(SeatStatus.HELD);

        when(seatAssignmentRepository.findByExpiresAtBeforeAndStatus(any(), any()))
                .thenReturn(Arrays.asList(expiredAssignment, expiredAssignment2));
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatRepository.findById(2L)).thenReturn(Optional.of(testSeat2));
        when(seatRepository.save(any(Seat.class))).thenAnswer(i -> i.getArgument(0));
        when(seatAssignmentRepository.save(any(SeatAssignment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        int result = seatExpiryService.releaseExpiredSeats();

        // Assert
        assertEquals(2, result);
        verify(seatRepository, times(2)).save(any(Seat.class));
        verify(seatAssignmentRepository, times(2)).save(any(SeatAssignment.class));
    }

    @Test
    void releaseExpiredSeats_SeatNotFound_ContinuesProcessing() {
        // Arrange
        when(seatAssignmentRepository.findByExpiresAtBeforeAndStatus(any(), any()))
                .thenReturn(Collections.singletonList(expiredAssignment));
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        int result = seatExpiryService.releaseExpiredSeats();

        // Assert
        assertEquals(0, result); // Failed to release
        verify(seatRepository, never()).save(any());
    }
}
