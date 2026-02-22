-- Create seat_history table for audit trail
CREATE TABLE IF NOT EXISTS seat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    details VARCHAR(500),
    CONSTRAINT fk_history_seat FOREIGN KEY (seat_id) REFERENCES seats(id)
);

CREATE INDEX IF NOT EXISTS idx_seat_id ON seat_history(seat_id);
CREATE INDEX IF NOT EXISTS idx_timestamp ON seat_history(timestamp);
