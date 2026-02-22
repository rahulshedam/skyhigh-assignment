package com.skyhigh.seat.scheduler;

import com.skyhigh.seat.service.WaitlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistProcessorSchedulerTest {

    @Mock
    private WaitlistService waitlistService;

    @InjectMocks
    private WaitlistProcessorScheduler waitlistProcessorScheduler;

    @Test
    void processWaitlists_CallsService() {
        // Arrange
        when(waitlistService.processAllWaitlists()).thenReturn(3);

        // Act
        waitlistProcessorScheduler.processWaitlists();

        // Assert
        verify(waitlistService).processAllWaitlists();
    }

    @Test
    void processWaitlists_ServiceThrowsException_HandlesGracefully() {
        // Arrange
        when(waitlistService.processAllWaitlists())
                .thenThrow(new RuntimeException("Test exception"));

        // Act - should not throw exception
        waitlistProcessorScheduler.processWaitlists();

        // Assert
        verify(waitlistService).processAllWaitlists();
    }

    @Test
    void processWaitlists_NoWaitlists() {
        // Arrange
        when(waitlistService.processAllWaitlists()).thenReturn(0);

        // Act
        waitlistProcessorScheduler.processWaitlists();

        // Assert
        verify(waitlistService).processAllWaitlists();
    }
}
