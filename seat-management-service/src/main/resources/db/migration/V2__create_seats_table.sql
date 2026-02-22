-- Create seats table
CREATE TABLE IF NOT EXISTS seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_number VARCHAR(5) NOT NULL,
    flight_id BIGINT NOT NULL,
    seat_class VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_seat_flight FOREIGN KEY (flight_id) REFERENCES flights(id)
);

CREATE INDEX IF NOT EXISTS idx_flight_id ON seats(flight_id);
CREATE INDEX IF NOT EXISTS idx_status ON seats(status);
CREATE INDEX IF NOT EXISTS idx_flight_status ON seats(flight_id, status);
-- H2 doesn't support IF NOT EXISTS for unique indexes directly in all versions, 
-- but we can use standard CREATE UNIQUE INDEX which typically fails if exists or use a named constraint.
-- Ideally we'd drop if exists, but for simple idempotency we'll assume standard creation.
-- Actually H2 2.1+ supports IF NOT EXISTS for indexes.
CREATE UNIQUE INDEX IF NOT EXISTS uk_flight_seat_number ON seats(flight_id, seat_number);
