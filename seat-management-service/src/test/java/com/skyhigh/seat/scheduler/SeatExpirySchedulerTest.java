package com.skyhigh.seat.scheduler;

import com.skyhigh.seat.service.SeatExpiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatExpirySchedulerTest {

    @Mock
    private SeatExpiryService seatExpiryService;

    @InjectMocks
    private SeatExpiryScheduler seatExpiryScheduler;

    @Test
    void releaseExpiredSeats_CallsService() {
        // Arrange
        when(seatExpiryService.releaseExpiredSeats()).thenReturn(5);

        // Act
        seatExpiryScheduler.releaseExpiredSeats();

        // Assert
        verify(seatExpiryService).releaseExpiredSeats();
    }

    @Test
    void releaseExpiredSeats_ServiceThrowsException_HandlesGracefully() {
        // Arrange
        when(seatExpiryService.releaseExpiredSeats())
                .thenThrow(new RuntimeException("Test exception"));

        // Act - should not throw exception
        seatExpiryScheduler.releaseExpiredSeats();

        // Assert
        verify(seatExpiryService).releaseExpiredSeats();
    }

    @Test
    void releaseExpiredSeats_NoExpiredSeats() {
        // Arrange
        when(seatExpiryService.releaseExpiredSeats()).thenReturn(0);

        // Act
        seatExpiryScheduler.releaseExpiredSeats();

        // Assert
        verify(seatExpiryService).releaseExpiredSeats();
    }
}
