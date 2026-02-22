-- Create seat_assignments table
CREATE TABLE IF NOT EXISTS seat_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    held_at TIMESTAMP,
    expires_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_assignment_seat FOREIGN KEY (seat_id) REFERENCES seats(id)
);

CREATE INDEX IF NOT EXISTS idx_seat_id ON seat_assignments(seat_id);
CREATE INDEX IF NOT EXISTS idx_passenger_id ON seat_assignments(passenger_id);
CREATE INDEX IF NOT EXISTS idx_expires_at_status ON seat_assignments(expires_at, status);
