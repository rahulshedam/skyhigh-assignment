package com.skyhigh.checkin;

import com.skyhigh.checkin.exception.CheckInException;
import com.skyhigh.checkin.exception.CheckInNotFoundException;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void testCheckInException() {
        CheckInException ex = new CheckInException("Error");
        assertEquals("Error", ex.getMessage());

        RuntimeException cause = new RuntimeException("Cause");
        CheckInException ex2 = new CheckInException("Error", cause);
        assertEquals("Error", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }

    @Test
    void testCheckInNotFoundException() {
        CheckInNotFoundException ex = new CheckInNotFoundException(1L);
        assertEquals("Check-in not found with ID: 1", ex.getMessage());
    }

    @Test
    void testServiceUnavailableException() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down");
        assertEquals("Service down", ex.getMessage());

        RuntimeException cause = new RuntimeException("Cause");
        ServiceUnavailableException ex2 = new ServiceUnavailableException("Service down", cause);
        assertEquals("Service down", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
}
