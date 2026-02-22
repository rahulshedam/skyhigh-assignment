package com.skyhigh.seat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private LockService lockService;

    @BeforeEach
    void setUp() {
        // No setup required for static fields
    }

    @Test
    void acquireLock_Success() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true);

        // Act
        RLock result = lockService.acquireLock("test-lock");

        // Assert
        assertNotNull(result);
        assertEquals(lock, result);
        verify(redissonClient).getLock("test-lock");
        verify(lock).tryLock(5L, 30L, TimeUnit.SECONDS);
    }

    @Test
    void acquireLock_Failure_ReturnsNull() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(false);

        // Act
        RLock result = lockService.acquireLock("test-lock");

        // Assert
        assertNull(result);
    }

    @Test
    void acquireLock_InterruptedException_ReturnsNull() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interrupt"));

        // Act
        RLock result = lockService.acquireLock("test-lock");

        // Assert
        assertNull(result);
    }

    @Test
    void releaseLock_Success() {
        // Arrange
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Act
        lockService.releaseLock(lock);

        // Assert
        verify(lock).unlock();
    }

    @Test
    void releaseLock_NotHeldByCurrentThread_DoesNotUnlock() {
        // Arrange
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        // Act
        lockService.releaseLock(lock);

        // Assert
        verify(lock, never()).unlock();
    }

    @Test
    void releaseLock_NullLock_DoesNothing() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> lockService.releaseLock(null));
    }

    @Test
    void executeWithLock_Success() throws Exception {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Act
        String result = lockService.executeWithLock("test-lock", () -> "test-result");

        // Assert
        assertEquals("test-result", result);
        verify(lock).unlock();
    }

    @Test
    void executeWithLock_LockNotAcquired_ThrowsException() throws InterruptedException {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> lockService.executeWithLock("test-lock", () -> "test-result"));
    }

    @Test
    void executeWithLock_TaskThrowsException_ReleasesLock() throws Exception {
        // Arrange
        when(redissonClient.getLock("test-lock")).thenReturn(lock);
        when(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> lockService.executeWithLock("test-lock", () -> {
            throw new RuntimeException("Task failed");
        }));
        verify(lock).unlock();
    }
}
