package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.SeatAlreadyHeldException;
import com.skyhigh.seat.exception.SeatNotFoundException;
import com.skyhigh.seat.exception.SeatUnavailableException;
import com.skyhigh.seat.model.dto.SeatConfirmRequest;
import com.skyhigh.seat.model.dto.SeatHoldRequest;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.entity.Seat;
import com.skyhigh.seat.model.entity.SeatAssignment;
import com.skyhigh.seat.model.enums.SeatAssignmentStatus;
import com.skyhigh.seat.model.enums.SeatClass;
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
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

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
    private SeatService seatService;

    private Seat testSeat;
    private SeatHoldRequest holdRequest;
    private SeatConfirmRequest confirmRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatService, "holdDurationSeconds", 120);

        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("1A");
        testSeat.setFlightId(100L);
        testSeat.setSeatClass(SeatClass.ECONOMY);
        testSeat.setStatus(SeatStatus.AVAILABLE);

        holdRequest = SeatHoldRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .build();

        confirmRequest = SeatConfirmRequest.builder()
                .passengerId("PASS123")
                .build();
    }

    @Test
    void holdSeat_Success() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any(SeatAssignment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        SeatResponse response = seatService.holdSeat(1L, holdRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("1A", response.getSeatNumber());
        assertEquals("PASS123", response.getPassengerId());
        assertEquals("BOOK456", response.getBookingReference());
        verify(lockService).acquireLock(anyString());
        verify(lockService).releaseLock(lock);
        verify(seatRepository).save(any(Seat.class));
        verify(seatAssignmentRepository).save(any(SeatAssignment.class));
        verify(seatHistoryRepository).save(any());
    }

    @Test
    void holdSeat_LockNotAcquired_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(null);

        // Act & Assert
        assertThrows(SeatAlreadyHeldException.class, () -> seatService.holdSeat(1L, holdRequest));
        verify(lockService, never()).releaseLock(any());
    }

    @Test
    void holdSeat_SeatNotFound_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> seatService.holdSeat(1L, holdRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void holdSeat_SeatNotAvailable_ThrowsException() {
        // Arrange
        testSeat.setStatus(SeatStatus.HELD);
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.holdSeat(1L, holdRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void confirmSeat_Success() {
        // Arrange
        testSeat.setStatus(SeatStatus.HELD);
        SeatAssignment assignment = SeatAssignment.builder()
                .id(1L)
                .seatId(1L)
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .status(SeatAssignmentStatus.HELD)
                .heldAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(120))
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatIdAndStatus(1L, SeatAssignmentStatus.HELD))
                .thenReturn(Optional.of(assignment));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any(SeatAssignment.class))).thenReturn(assignment);

        // Act
        SeatResponse response = seatService.confirmSeat(1L, confirmRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PASS123", response.getPassengerId());
        verify(lockService).releaseLock(lock);
        verify(seatRepository).save(any(Seat.class));
        verify(seatAssignmentRepository).save(any(SeatAssignment.class));
    }

    @Test
    void confirmSeat_NoActiveHold_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatIdAndStatus(1L, SeatAssignmentStatus.HELD))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.confirmSeat(1L, confirmRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void confirmSeat_WrongPassenger_ThrowsException() {
        // Arrange
        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(1L)
                .passengerId("DIFFERENT_PASSENGER")
                .status(SeatAssignmentStatus.HELD)
                .expiresAt(LocalDateTime.now().plusSeconds(120))
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatIdAndStatus(1L, SeatAssignmentStatus.HELD))
                .thenReturn(Optional.of(assignment));

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.confirmSeat(1L, confirmRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void confirmSeat_HoldExpired_ThrowsException() {
        // Arrange
        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(1L)
                .passengerId("PASS123")
                .status(SeatAssignmentStatus.HELD)
                .expiresAt(LocalDateTime.now().minusSeconds(10))
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatIdAndStatus(1L, SeatAssignmentStatus.HELD))
                .thenReturn(Optional.of(assignment));

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.confirmSeat(1L, confirmRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void cancelSeat_Success() {
        // Arrange
        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(1L)
                .passengerId("PASS123")
                .status(SeatAssignmentStatus.CONFIRMED)
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatId(1L)).thenReturn(Optional.of(assignment));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(seatAssignmentRepository.save(any(SeatAssignment.class))).thenReturn(assignment);

        // Act
        seatService.cancelSeat(1L, "PASS123");

        // Assert
        verify(lockService).releaseLock(lock);
        verify(seatRepository).save(any(Seat.class));
        verify(seatAssignmentRepository).save(any(SeatAssignment.class));
        verify(seatHistoryRepository).save(any());
    }

    @Test
    void cancelSeat_NoAssignment_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.cancelSeat(1L, "PASS123"));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void getSeatStatus_Success() {
        // Arrange
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatId(1L)).thenReturn(Optional.empty());

        // Act
        SeatResponse response = seatService.getSeatStatus(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("1A", response.getSeatNumber());
        assertNull(response.getPassengerId());
    }

    @Test
    void getSeatStatus_SeatNotFound_ThrowsException() {
        // Arrange
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> seatService.getSeatStatus(1L));
    }

    @Test
    void cancelSeat_WrongPassenger_ThrowsException() {
        // Arrange
        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(1L)
                .passengerId("DIFFERENT_PASSENGER")
                .status(SeatAssignmentStatus.CONFIRMED)
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.of(testSeat));
        when(seatAssignmentRepository.findBySeatId(1L)).thenReturn(Optional.of(assignment));

        // Act & Assert
        assertThrows(SeatUnavailableException.class, () -> seatService.cancelSeat(1L, "PASS123"));
        verify(lockService).releaseLock(lock);
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void cancelSeat_LockNotAcquired_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(null);

        // Act & Assert
        assertThrows(SeatAlreadyHeldException.class, () -> seatService.cancelSeat(1L, "PASS123"));
        verify(lockService, never()).releaseLock(any());
    }

    @Test
    void confirmSeat_LockNotAcquired_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(null);

        // Act & Assert
        assertThrows(SeatAlreadyHeldException.class, () -> seatService.confirmSeat(1L, confirmRequest));
        verify(lockService, never()).releaseLock(any());
    }

    @Test
    void confirmSeat_SeatNotFound_ThrowsException() {
        // Arrange
        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SeatNotFoundException.class, () -> seatService.confirmSeat(1L, confirmRequest));
        verify(lockService).releaseLock(lock);
    }

    @Test
    void holdSeat_shouldReturnSuccess_whenAlreadyHeldBySamePassenger() {
        // Arrange
        Long seatId = 1L;
        String passengerId = "PASS123";
        SeatHoldRequest request = new SeatHoldRequest(passengerId, "BOOK456");

        Seat seat = new Seat();
        seat.setId(seatId);
        seat.setSeatNumber("1A");
        seat.setStatus(SeatStatus.HELD);

        SeatAssignment assignment = SeatAssignment.builder()
                .seatId(seatId)
                .passengerId(passengerId)
                .bookingReference("BOOK456")
                .status(SeatAssignmentStatus.HELD)
                .heldAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(120))
                .build();

        when(lockService.acquireLock(anyString())).thenReturn(lock);
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(seatAssignmentRepository.findBySeatIdAndStatus(seatId, SeatAssignmentStatus.HELD))
                .thenReturn(Optional.of(assignment));

        // Act
        SeatResponse response = seatService.holdSeat(seatId, request);

        // Assert
        assertNotNull(response);
        assertEquals(SeatStatus.HELD, response.getStatus());
        assertEquals(passengerId, response.getPassengerId());
        verify(seatRepository, never()).save(any(Seat.class));
        verify(seatAssignmentRepository, never()).save(any(SeatAssignment.class));
        verify(lockService).releaseLock(lock);
    }
}
