-- Create baggage_records table
CREATE TABLE IF NOT EXISTS baggage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    baggage_reference VARCHAR(50) NOT NULL UNIQUE,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    weight DECIMAL(10, 2) NOT NULL,
    excess_weight DECIMAL(10, 2),
    excess_fee DECIMAL(10, 2),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_baggage_passenger ON baggage_records(passenger_id);
CREATE INDEX idx_baggage_booking ON baggage_records(booking_reference);
CREATE INDEX idx_baggage_status ON baggage_records(status);
