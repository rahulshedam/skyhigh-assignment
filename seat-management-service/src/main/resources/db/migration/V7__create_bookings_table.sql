-- Create bookings table
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(10) NOT NULL,
    passenger_id VARCHAR(100) NOT NULL,
    flight_id BIGINT NOT NULL,
    passenger_name VARCHAR(255) NOT NULL,
    passenger_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_bookings_flight FOREIGN KEY (flight_id) REFERENCES flights(id),
    CONSTRAINT uk_booking_passenger UNIQUE (booking_reference, passenger_id)
);

-- Create index on booking_reference for faster lookups
CREATE INDEX idx_bookings_booking_reference ON bookings(booking_reference);

-- Create index on passenger_id for faster lookups
CREATE INDEX idx_bookings_passenger_id ON bookings(passenger_id);

-- Create index on flight_id for faster lookups
CREATE INDEX idx_bookings_flight_id ON bookings(flight_id);
