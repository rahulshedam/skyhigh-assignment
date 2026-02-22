-- Seed initial flight and seat data for testing

-- Insert sample flights (idempotent)
INSERT INTO flights 
(flight_number, departure_time, arrival_time, origin, destination, aircraft_type, created_at, updated_at)
VALUES 
('SK101', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 
          DATEADD('HOUR', 2, DATEADD('DAY', 1, CURRENT_TIMESTAMP)), 
 'JFK', 'LAX', 'Boeing 737', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('SK102', DATEADD('DAY', 2, CURRENT_TIMESTAMP), 
          DATEADD('HOUR', 3, DATEADD('DAY', 2, CURRENT_TIMESTAMP)), 
 'LAX', 'SFO', 'Airbus A320', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('SK103', DATEADD('DAY', 3, CURRENT_TIMESTAMP), 
          DATEADD('HOUR', 5, DATEADD('DAY', 3, CURRENT_TIMESTAMP)), 
 'SFO', 'ORD', 'Boeing 777', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);



----------------------------------------------------
-- Seats for SK101 (25 rows × 6 seats)
----------------------------------------------------
INSERT INTO seats (seat_number, flight_id, seat_class, status, created_at, updated_at)
SELECT 
    CONCAT(rows.row_num, letters.seat_letter),
    1,
    'ECONOMY',
    'AVAILABLE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT X AS row_num FROM SYSTEM_RANGE(1, 25)
) rows
CROSS JOIN (
    SELECT 'A' AS seat_letter UNION ALL
    SELECT 'B' UNION ALL
    SELECT 'C' UNION ALL
    SELECT 'D' UNION ALL
    SELECT 'E' UNION ALL
    SELECT 'F'
) letters
WHERE NOT EXISTS (
    SELECT 1 FROM seats s 
    WHERE s.flight_id = 1 
    AND s.seat_number = CONCAT(rows.row_num, letters.seat_letter)
);



----------------------------------------------------
-- Seats for SK102 (30 rows × 6 seats)
----------------------------------------------------
INSERT INTO seats (seat_number, flight_id, seat_class, status, created_at, updated_at)
SELECT 
    CONCAT(rows.row_num, letters.seat_letter),
    2,
    'ECONOMY',
    'AVAILABLE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT X AS row_num FROM SYSTEM_RANGE(1, 30)
) rows
CROSS JOIN (
    SELECT 'A' AS seat_letter UNION ALL
    SELECT 'B' UNION ALL
    SELECT 'C' UNION ALL
    SELECT 'D' UNION ALL
    SELECT 'E' UNION ALL
    SELECT 'F'
) letters
WHERE NOT EXISTS (
    SELECT 1 FROM seats s 
    WHERE s.flight_id = 2 
    AND s.seat_number = CONCAT(rows.row_num, letters.seat_letter)
);



----------------------------------------------------
-- Business seats for SK103 (Rows 1–5 × 4 seats)
----------------------------------------------------
INSERT INTO seats (seat_number, flight_id, seat_class, status, created_at, updated_at)
SELECT 
    CONCAT(rows.row_num, letters.seat_letter),
    3,
    'BUSINESS',
    'AVAILABLE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT X AS row_num FROM SYSTEM_RANGE(1, 5)
) rows
CROSS JOIN (
    SELECT 'A' AS seat_letter UNION ALL
    SELECT 'B' UNION ALL
    SELECT 'C' UNION ALL
    SELECT 'D'
) letters
WHERE NOT EXISTS (
    SELECT 1 FROM seats s 
    WHERE s.flight_id = 3 
    AND s.seat_number = CONCAT(rows.row_num, letters.seat_letter)
);



----------------------------------------------------
-- Economy seats for SK103 (Rows 6–51 × 6 seats)
----------------------------------------------------
INSERT INTO seats (seat_number, flight_id, seat_class, status, created_at, updated_at)
SELECT 
    CONCAT(rows.row_num, letters.seat_letter),
    3,
    'ECONOMY',
    'AVAILABLE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    SELECT X + 5 AS row_num FROM SYSTEM_RANGE(1, 46)
) rows
CROSS JOIN (
    SELECT 'A' AS seat_letter UNION ALL
    SELECT 'B' UNION ALL
    SELECT 'C' UNION ALL
    SELECT 'D' UNION ALL
    SELECT 'E' UNION ALL
    SELECT 'F'
) letters
WHERE NOT EXISTS (
    SELECT 1 FROM seats s 
    WHERE s.flight_id = 3 
    AND s.seat_number = CONCAT(rows.row_num, letters.seat_letter)
);
