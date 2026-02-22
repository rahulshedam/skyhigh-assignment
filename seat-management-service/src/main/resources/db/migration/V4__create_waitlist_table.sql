-- Create waitlist table
CREATE TABLE IF NOT EXISTS waitlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    assigned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_waitlist_seat FOREIGN KEY (seat_id) REFERENCES seats(id)
);

CREATE INDEX IF NOT EXISTS idx_seat_status_joined ON waitlist(seat_id, status, joined_at);
CREATE INDEX IF NOT EXISTS idx_passenger_id ON waitlist(passenger_id);
