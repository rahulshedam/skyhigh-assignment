# SkyHigh Application – Test Data

Use this data to test the UI and APIs. All values match the seed data and runtime initialization.

---

## 1. Pre-seeded data (after app start)

### Flights

| Flight ID | Flight Number | Route    | Aircraft   |
|-----------|---------------|----------|------------|
| 1         | SK101         | JFK → LAX | Boeing 737 |
| 2         | SK102         | LAX → SFO | Airbus A320 |
| 3         | SK103         | SFO → ORD | Boeing 777 |

- **Flight 1** (SK101): 25 rows × 6 seats (A–F) = 150 economy seats  
- **Flight 2** (SK102): 30 rows × 6 seats = 180 economy seats  
- **Flight 3** (SK103): 5 rows business (A–D) + 46 rows economy (A–F)

### Bookings (created at startup)

Valid **booking reference + passenger ID (email)** pairs for verification and check-in:

| Booking Reference | Passenger ID (email)      | Passenger Name |
|-------------------|---------------------------|----------------|
| ABC123            | john.doe@email.com        | John Doe       |
| XYZ789            | jane.smith@email.com     | Jane Smith     |
| SKY123            | bob.wilson@email.com     | Bob Wilson     |
| TEST01            | test@test.com            | Test User      |
| DEMO99            | demo@demo.com            | Demo User      |

Use these in the **Home** page “Search” and for all check-in/seat/baggage/payment APIs.

---

## 2. Quick copy-paste for UI

**Verify booking (e.g. on Home page):**

- Booking ref: `ABC123`  
- Passenger: `john.doe@email.com`

Other combinations: `XYZ789` / `jane.smith@email.com`, `SKY123` / `bob.wilson@email.com`, `TEST01` / `test@test.com`, `DEMO99` / `demo@demo.com`.

After verification you get flight **SK101** (flightId **1**). Open the seat map, pick any **AVAILABLE** seat, then go through check-in.

---

## 3. Baggage scenarios

- **No excess fee:** weight ≤ 25 kg (e.g. `20`, `25`) → check-in can complete without payment.
- **Excess baggage:** weight > 25 kg (e.g. `30`, `35`) → check-in goes to **WAITING_FOR_PAYMENT**; pay via Payment Service, then complete with `paymentId`.

Fee formula (typical): `(weight - 25) × 10` (e.g. 30 kg → 50).

---

## 4. Sample API requests

Base URLs (when running locally or via Docker):

- Seat Management: `http://localhost:8091`
- Check-in: `http://localhost:8082`
- Baggage: `http://localhost:8083`
- Payment: `http://localhost:8084`
- Notification: `http://localhost:8085`

If using the frontend proxy (port 3000), replace with `http://localhost:3000/api/...` and the correct path per service (e.g. `/api/bookings/verify`, `/api/seats/...`).

### 4.1 Booking verification

```http
POST http://localhost:8091/api/bookings/verify
Content-Type: application/json

{
  "bookingReference": "ABC123",
  "passengerId": "john.doe@email.com"
}
```

### 4.2 Get seat map (get a valid seat ID)

```http
GET http://localhost:8091/api/seats/flights/1/seatmap
```

Use any seat with `"status": "AVAILABLE"` and note its `id` for hold/check-in.

### 4.3 Hold seat

```http
POST http://localhost:8091/api/seats/{seatId}/hold
Content-Type: application/json

{
  "passengerId": "john.doe@email.com",
  "bookingReference": "ABC123"
}
```

Replace `{seatId}` with an available seat `id` from the seatmap (e.g. `1`, `2`, `145`).

### 4.4 Confirm seat

```http
POST http://localhost:8091/api/seats/{seatId}/confirm
Content-Type: application/json

{
  "passengerId": "john.doe@email.com",
  "bookingReference": "ABC123"
}
```

### 4.5 Start check-in

Use the same `seatId` you held/confirmed (must be held or confirmed for this passenger/booking).

```http
POST http://localhost:8082/api/checkin/start
Content-Type: application/json

{
  "bookingReference": "ABC123",
  "passengerId": "john.doe@email.com",
  "flightId": 1,
  "seatId": 145,
  "baggageWeight": 20.0
}
```

- `baggageWeight` optional; if omitted, treated as 0. Use `20` for no fee, `30` for excess (payment flow).

### 4.6 Update baggage (in-progress check-in)

```http
POST http://localhost:8082/api/checkin/{checkinId}/baggage
Content-Type: application/json

{
  "weight": 30.0
}
```

If weight > 25 kg, check-in moves to **WAITING_FOR_PAYMENT**.

### 4.7 Complete check-in (with optional payment)

When status is **WAITING_FOR_PAYMENT**, first process payment (see 4.8), then:

```http
POST http://localhost:8082/api/checkin/{checkinId}/complete
Content-Type: application/json

{
  "paymentId": "PAY-xxx"
}
```

Use the `paymentReference` (or equivalent) returned by the Payment Service as `paymentId`.  
When no payment is required, you can call complete with an empty body or without `paymentId`.

### 4.8 Process payment (excess baggage)

```http
POST http://localhost:8084/api/payments/process
Content-Type: application/json

{
  "passengerId": "john.doe@email.com",
  "bookingReference": "ABC123",
  "amount": 50.00,
  "currency": "USD",
  "paymentType": "EXCESS_BAGGAGE"
}
```

- `paymentType`: `EXCESS_BAGGAGE` | `SEAT_UPGRADE` | `OTHER`  
- Optional: `"simulateFailure": true` to test failure (e.g. INSUFFICIENT_FUNDS).

### 4.9 Baggage validation (standalone)

```http
POST http://localhost:8083/api/baggage/validate
Content-Type: application/json

{
  "passengerId": "john.doe@email.com",
  "bookingReference": "ABC123",
  "weight": 28.5
}
```

### 4.10 Notifications by booking

```http
GET http://localhost:8085/api/notifications/booking/ABC123
```

### 4.11 Send test notification

```http
POST http://localhost:8085/api/notifications/test
Content-Type: application/json

{
  "recipientId": "john.doe@email.com",
  "subject": "Test",
  "content": "Test notification",
  "bookingReference": "ABC123"
}
```

---

## 5. Test scenarios (end-to-end)

| Scenario              | Booking ref | Passenger ID           | Baggage (kg) | Notes                                      |
|-----------------------|------------|------------------------|-------------|--------------------------------------------|
| Happy path, no fee    | ABC123     | john.doe@email.com    | 20          | Verify → seat map → hold → confirm → start check-in (20 kg) → complete |
| Excess baggage        | XYZ789     | jane.smith@email.com  | 30          | Same flow; after start/update baggage, complete payment then complete with `paymentId` |
| Borderline 25 kg      | SKY123     | bob.wilson@email.com  | 25          | No excess fee                              |
| Payment failure test  | TEST01     | test@test.com         | 35          | Use Payment with `simulateFailure: true`   |

---

## 6. Seat IDs

Seat IDs are auto-generated. Always:

1. Call `GET /api/seats/flights/1/seatmap` (or 2, 3).
2. Pick a seat with `"status": "AVAILABLE"`.
3. Use that seat’s `id` in hold, confirm, and check-in.

For flight **1** (SK101), IDs are typically in the low range (e.g. 1–150); exact values depend on DB state.

---

## 7. Cancellation

- **Cancel seat:** `DELETE /api/seats/{seatId}/cancel?passengerId=john.doe@email.com`
- **Cancel check-in:** `POST /api/checkin/{checkinId}/cancel`

Use the same `passengerId` (email) that holds/owns the seat or check-in.
