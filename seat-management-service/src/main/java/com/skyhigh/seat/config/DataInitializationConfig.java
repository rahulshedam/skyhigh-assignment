package com.skyhigh.seat.config;

import com.skyhigh.seat.model.entity.Booking;
import com.skyhigh.seat.model.entity.Flight;
import com.skyhigh.seat.repository.BookingRepository;
import com.skyhigh.seat.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;

/**
 * Initializes sample booking data for testing
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializationConfig {

    @Bean
    @Order(2) // Run after flight/seat initialization if exists
    public CommandLineRunner initializeBookingData(
            BookingRepository bookingRepository,
            FlightRepository flightRepository) {
        
        return args -> {
            if (bookingRepository.count() > 0) {
                log.info("Booking data already exists, skipping initialization");
                return;
            }

            log.info("Initializing sample booking data...");

            // Get the first available flight (should be SK101 from migration)
            Flight flight = flightRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No flights found in database. Please ensure V6 migration has run."));

            // Create sample bookings
            createBooking(bookingRepository, flight, "ABC123", "john.doe@email.com", "John Doe");
            createBooking(bookingRepository, flight, "XYZ789", "jane.smith@email.com", "Jane Smith");
            createBooking(bookingRepository, flight, "SKY123", "bob.wilson@email.com", "Bob Wilson");
            createBooking(bookingRepository, flight, "TEST01", "test@test.com", "Test User");
            createBooking(bookingRepository, flight, "DEMO99", "demo@demo.com", "Demo User");

            log.info("Sample booking data initialized successfully");
        };
    }

    private void createBooking(BookingRepository repository, Flight flight, 
                              String reference, String email, String name) {
        Booking booking = Booking.builder()
                .bookingReference(reference)
                .passengerId(email)
                .flight(flight)
                .passengerName(name)
                .passengerEmail(email)
                .status(Booking.BookingStatus.ACTIVE)
                .build();
        repository.save(booking);
        log.info("Created booking: {} for passenger: {}", reference, name);
    }
}
