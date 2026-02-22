package com.skyhigh.seat.service;

import com.skyhigh.seat.exception.ResourceNotFoundException;
import com.skyhigh.seat.model.dto.BookingVerificationRequest;
import com.skyhigh.seat.model.dto.BookingVerificationResponse;
import com.skyhigh.seat.model.entity.Booking;
import com.skyhigh.seat.model.entity.Flight;
import com.skyhigh.seat.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for booking verification operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;

    /**
     * Verify a booking reference and passenger ID
     * 
     * @param request the verification request
     * @return booking verification response with flight details
     * @throws ResourceNotFoundException if booking is not found
     */
    @Transactional(readOnly = true)
    public BookingVerificationResponse verifyBooking(BookingVerificationRequest request) {
        log.info("Verifying booking: {} for passenger: {}", 
                request.bookingReference(), request.passengerId());

        Booking booking = bookingRepository
                .findByBookingReferenceAndPassengerId(
                        request.bookingReference(), 
                        request.passengerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found for reference: " + request.bookingReference() 
                        + " and passenger: " + request.passengerId()));

        Flight flight = booking.getFlight();

        return BookingVerificationResponse.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .bookingReference(booking.getBookingReference())
                .passengerId(booking.getPassengerId())
                .valid(booking.getStatus() == Booking.BookingStatus.ACTIVE)
                .build();
    }
}
