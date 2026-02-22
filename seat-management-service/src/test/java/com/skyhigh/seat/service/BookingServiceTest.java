package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.ResourceNotFoundException;
import com.skyhigh.seat.model.dto.BookingVerificationRequest;
import com.skyhigh.seat.model.dto.BookingVerificationResponse;
import com.skyhigh.seat.model.entity.Booking;
import com.skyhigh.seat.model.entity.Flight;
import com.skyhigh.seat.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private Flight testFlight;
    private Booking activeBooking;
    private Booking cancelledBooking;
    private BookingVerificationRequest verificationRequest;

    @BeforeEach
    void setUp() {
        testFlight = Flight.builder()
                .id(100L)
                .flightNumber("SK1234")
                .origin("BOM")
                .destination("DEL")
                .departureTime(LocalDateTime.now().plusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(4))
                .aircraftType("Boeing 737")
                .build();

        activeBooking = Booking.builder()
                .id(1L)
                .bookingReference("BOOK456")
                .passengerId("PASS123")
                .flight(testFlight)
                .passengerName("John Doe")
                .passengerEmail("john@example.com")
                .status(Booking.BookingStatus.ACTIVE)
                .build();

        cancelledBooking = Booking.builder()
                .id(2L)
                .bookingReference("BOOK789")
                .passengerId("PASS123")
                .flight(testFlight)
                .passengerName("John Doe")
                .passengerEmail("john@example.com")
                .status(Booking.BookingStatus.CANCELLED)
                .build();

        verificationRequest = new BookingVerificationRequest("BOOK456", "PASS123");
    }

    @Test
    void verifyBooking_Success_ActiveBooking() {
        when(bookingRepository.findByBookingReferenceAndPassengerId("BOOK456", "PASS123"))
                .thenReturn(Optional.of(activeBooking));

        BookingVerificationResponse response = bookingService.verifyBooking(verificationRequest);

        assertNotNull(response);
        assertEquals(100L, response.getFlightId());
        assertEquals("SK1234", response.getFlightNumber());
        assertEquals("BOM", response.getOrigin());
        assertEquals("DEL", response.getDestination());
        assertEquals("BOOK456", response.getBookingReference());
        assertEquals("PASS123", response.getPassengerId());
        assertTrue(response.isValid());

        verify(bookingRepository).findByBookingReferenceAndPassengerId("BOOK456", "PASS123");
    }

    @Test
    void verifyBooking_NotFound_ThrowsResourceNotFoundException() {
        when(bookingRepository.findByBookingReferenceAndPassengerId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        BookingVerificationRequest request = new BookingVerificationRequest("NON_EXISTENT", "PASS123");

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookingService.verifyBooking(request));

        assertTrue(ex.getMessage().contains("NON_EXISTENT"));
        assertTrue(ex.getMessage().contains("PASS123"));
    }

    @Test
    void verifyBooking_CancelledBooking_ReturnsInvalid() {
        when(bookingRepository.findByBookingReferenceAndPassengerId("BOOK789", "PASS123"))
                .thenReturn(Optional.of(cancelledBooking));

        BookingVerificationRequest request = new BookingVerificationRequest("BOOK789", "PASS123");
        BookingVerificationResponse response = bookingService.verifyBooking(request);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("BOOK789", response.getBookingReference());
    }

    @Test
    void verifyBooking_CheckedInBooking_ReturnsInvalid() {
        Booking checkedInBooking = Booking.builder()
                .id(3L)
                .bookingReference("BOOK999")
                .passengerId("PASS123")
                .flight(testFlight)
                .passengerName("John Doe")
                .passengerEmail("john@example.com")
                .status(Booking.BookingStatus.CHECKED_IN)
                .build();

        when(bookingRepository.findByBookingReferenceAndPassengerId("BOOK999", "PASS123"))
                .thenReturn(Optional.of(checkedInBooking));

        BookingVerificationRequest request = new BookingVerificationRequest("BOOK999", "PASS123");
        BookingVerificationResponse response = bookingService.verifyBooking(request);

        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    void verifyBooking_CorrectFlightDetailsReturned() {
        when(bookingRepository.findByBookingReferenceAndPassengerId("BOOK456", "PASS123"))
                .thenReturn(Optional.of(activeBooking));

        BookingVerificationResponse response = bookingService.verifyBooking(verificationRequest);

        assertEquals(testFlight.getId(), response.getFlightId());
        assertEquals(testFlight.getFlightNumber(), response.getFlightNumber());
        assertEquals(testFlight.getOrigin(), response.getOrigin());
        assertEquals(testFlight.getDestination(), response.getDestination());
    }
}
