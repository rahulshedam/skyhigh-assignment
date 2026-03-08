package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.model.dto.WaitlistJoinRequest;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.entity.Waitlist;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.model.enums.WaitlistStatus;
import com.skyhigh.seat.repository.SeatAssignmentRepository;
import com.skyhigh.seat.repository.SeatHistoryRepository;
import com.skyhigh.seat.repository.SeatRepository;
import com.skyhigh.seat.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatAssignmentRepository seatAssignmentRepository;

    @Mock
    private SeatHistoryRepository seatHistoryRepository;

    @Mock
    private LockService lockService;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private RLock lock;

    @InjectMocks
    private WaitlistService waitlistService;

    private Seat testSeat;
    private WaitlistJoinRequest joinRequest;
    private Waitlist testWaitlist;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waitlistService, "holdDurationSeconds", 120);

        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("1A");
        testSeat.setFlightId(100L);
        testSeat.setStatus(SeatStatus.AVAILABLE);

        joinRequest = WaitlistJoinRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .build();

        testWaitlist = Waitlist.builder()
                .id(1L)
                .seatId(1L)
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .status(WaitlistStatus.WAITING)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void joinWaitlist_Success() {
        // Arrange
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(waitlistRepository.save(any(Waitlist.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Waitlist result = waitlistService.joinWaitlist(1L, joinRequest);

        // Assert
        assertNotNull(result);
        assertEquals("PASS123", result.getPassengerId());
        assertEquals("BOOK456", result.getBookingReference());
        assertEquals(WaitlistStatus.WAITING, result.getStatus());
        verify(waitlistRepository).save(any(Waitlist.class));
    }

    @Test
    void joinWaitlist_SeatNotFound_ThrowsException() {
        // Arrange
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> waitlistService.joinWaitlist(1L, joinRequest));
    }

    @Test
    void processWaitlistForSeat_NoLock_ReturnsEarly() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(null);

        // Act
        waitlistService.processWaitlistForSeat(1L);

        // Assert
        verify(seatRepository, never()).findById(any());
    }

    @Test
    void processWaitlistForSeat_SeatNotAvailable_ReturnsEarly() {
        // Arrange
        testSeat.setStatus(SeatStatus.HELD);
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));

        // Act
        waitlistService.processWaitlistForSeat(1L);

        // Assert
        verify(lockService).releaseLock(lock);
        verify(waitlistRepository, never()).findWaitingBySeatId(any());
    }

    @Test
    void processWaitlistForSeat_NoWaitingPassengers_ReturnsEarly() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(waitlistRepository.findWaitingBySeatId(1L)).thenReturn(Collections.emptyList());

        // Act
        waitlistService.processWaitlistForSeat(1L);

        // Assert
        verify(lockService).releaseLock(lock);
        verify(seatRepository, never()).save(any());
    }

    @Test
    void processWaitlistForSeat_AssignsToFirstInLine() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(waitlistRepository.findWaitingBySeatId(1L))
                .thenReturn(Collections.singletonList(testWaitlist));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any(SeatAssignment.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(Waitlist.class))).thenReturn(testWaitlist);

        // Act
        waitlistService.processWaitlistForSeat(1L);

        // Assert
        verify(seatRepository).save(any(Seat.class));
        verify(seatAssignmentRepository).save(any(SeatAssignment.class));
        verify(waitlistRepository).save(any(Waitlist.class));
        verify(seatHistoryRepository).save(any());
        verify(lockService).releaseLock(lock);
    }

    @Test
    void processAllWaitlists_NoAvailableSeats() {
        // Arrange
        when(seatRepository.findByStatus(SeatStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

        // Act
        int result = waitlistService.processAllWaitlists();

        // Assert
        assertEquals(0, result);
    }

    @Test
    void processAllWaitlists_ProcessesMultipleSeats() {
        // Arrange
        Seat seat2 = new Seat();
        seat2.setId(2L);
        seat2.setStatus(SeatStatus.AVAILABLE);

        when(seatRepository.findByStatus(SeatStatus.AVAILABLE))
                .thenReturn(Arrays.asList(testSeat, seat2));
        when(waitlistRepository.findWaitingBySeatId(1L))
                .thenReturn(Collections.singletonList(testWaitlist));
        when(waitlistRepository.findWaitingBySeatId(2L))
                .thenReturn(Collections.emptyList());
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(any())).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any())).thenReturn(testWaitlist);

        // Act
        int result = waitlistService.processAllWaitlists();

        // Assert
        assertEquals(1, result); // Only seat 1 had waiting passengers
    }

    @Test
    void getPassengerWaitlists_Success() {
        // Arrange
        when(waitlistRepository.findByPassengerId("PASS123"))
                .thenReturn(Collections.singletonList(testWaitlist));

        // Act
        List<Waitlist> result = waitlistService.getPassengerWaitlists("PASS123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PASS123", result.get(0).getPassengerId());
    }

    @Test
    void removeFromWaitlist_Success() {
        // Arrange
        when(waitlistRepository.findById(1L)).thenReturn(Optional.of(testWaitlist));
        when(waitlistRepository.save(any(Waitlist.class))).thenReturn(testWaitlist);

        // Act
        waitlistService.removeFromWaitlist(1L);

        // Assert
        verify(waitlistRepository).save(any(Waitlist.class));
    }

    @Test
    void removeFromWaitlist_NotFound_ThrowsException() {
        // Arrange
        when(waitlistRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.skyhigh.seat.exception.WaitlistNotFoundException.class, () -> waitlistService.removeFromWaitlist(1L));
    }
}
