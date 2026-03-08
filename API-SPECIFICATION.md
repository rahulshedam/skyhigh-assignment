# API Specification
## SkyHigh Core – Digital Check-In System

This document defines all REST API endpoints built for the SkyHigh digital check-in platform.

---

## Services Overview

| Service | Base Path | Description |
|---------|-----------|-------------|
| Seat Management | `/api/seats`, `/api/bookings` | Seat maps, holds, booking verification |
| Baggage | `/api/baggage` | Baggage validation and fees |
| Check-in | `/api/checkin` | Orchestrated check-in workflow |
| Payment | `/api/payments` | Payment processing |
| Notification | `/api/notifications` | Notification management |

---

## 1. Seat Management Service

### 1.1 Booking Verification

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/bookings/verify` | Verify booking reference and passenger ID; returns flight details |

**Request body:** `BookingVerificationRequest` (bookingReference, passengerId)

---

### 1.2 Seat Map

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/seats/flights/{flightId}/seatmap` | Get complete seat map for a flight with availability status |

---

### 1.3 Seat Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/seats/{seatId}/hold` | Hold a seat for a passenger (120-second expiry) |
| `POST` | `/api/seats/{seatId}/confirm` | Confirm a held seat assignment |
| `DELETE` | `/api/seats/{seatId}/cancel` | Cancel a confirmed seat assignment |
| `GET` | `/api/seats/{seatId}/status` | Get current status of a seat |

**Cancel query param:** `passengerId` (required for cancel). Cancels a confirmed seat or releases a hold; after release, the waitlist processor may assign the seat to the next waitlisted passenger (FIFO).

---

### 1.4 Waitlist

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/seats/{seatId}/waitlist` | Add passenger to waitlist for a seat |
| `GET` | `/api/seats/waitlist/{passengerId}` | Get all waitlist entries for a passenger |
| `DELETE` | `/api/seats/waitlist/{waitlistId}` | Remove passenger from waitlist |

---

## 2. Baggage Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/baggage/validate` | Validate baggage weight and calculate excess fees |
| `GET` | `/api/baggage/{baggageReference}` | Get baggage details by reference |
| `GET` | `/api/baggage/passenger/{passengerId}` | Get all baggage for a passenger |
| `GET` | `/api/baggage/booking/{bookingReference}` | Get all baggage for a booking |

---

## 3. Check-in Service

The check-in flow supports **pause for payment** when excess baggage is required (weight > 25 kg). In that case the status becomes `WAITING_FOR_PAYMENT`; the client processes payment via the Payment Service and then **resumes** by calling the complete endpoint with the payment reference.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/checkin/start` | Start check-in (seat hold + baggage validation) |
| `GET` | `/api/checkin/{checkinId}` | Get check-in status |
| `POST` | `/api/checkin/{checkinId}/complete` | Complete check-in (after payment if required); send `paymentId` when resuming from WAITING_FOR_PAYMENT |
| `POST` | `/api/checkin/{checkinId}/baggage` | Update baggage weight for in-progress check-in |
| `POST` | `/api/checkin/{checkinId}/cancel` | Cancel an in-progress check-in (releases seat hold via Seat Management; idempotent if already cancelled) |

---

## Validation and Business Rules

- **Check-in:** `bookingReference`, `passengerId`, `flightId`, `seatId` required; `baggageWeight` optional, must be ≥ 0 if present. Excess baggage fee applies when weight > 25 kg.
- **Seat:** For hold and confirm: `passengerId`, `bookingReference` required. For cancel: `passengerId` query param required.
- **Baggage:** `passengerId`, `bookingReference`, `weight` (positive) required. Excess fee when weight > 25 kg (e.g. (weight − 25) × 10).
- **Payment:** Required fields depend on service (e.g. amount, passenger/booking reference); see Payment Service API.

---

## 4. Payment Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/payments/process` | Process a payment (excess baggage, etc.) |
| `GET` | `/api/payments/{paymentReference}` | Get payment details by reference |
| `GET` | `/api/payments/passenger/{passengerId}` | Get all payments for a passenger |
| `GET` | `/api/payments/booking/{bookingReference}` | Get all payments for a booking |

---

## 5. Notification Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notifications/hello` | Health check |
| `GET` | `/api/notifications` | Get all notifications (paginated) |
| `GET` | `/api/notifications/{id}` | Get notification by ID |
| `GET` | `/api/notifications/recipient/{recipientId}` | Get notifications by recipient |
| `GET` | `/api/notifications/status/{status}` | Get notifications by status |
| `GET` | `/api/notifications/booking/{bookingReference}` | Get notifications by booking reference |
| `GET` | `/api/notifications/stats` | Get notification statistics |
| `POST` | `/api/notifications/test` | Send a test notification (demo/testing) |

**Notification list query params:** `page` (default 0), `size` (default 10), `sortBy` (default createdAt), `sortDir` (default DESC)

---

## Error Responses

APIs use standard HTTP status codes for errors. The following apply across services where relevant:

| Status | Meaning | When used |
|--------|----------|-----------|
| **400 Bad Request** | Validation failed or invalid state transition | Invalid request body (e.g. missing/invalid fields), business rule violation (e.g. cannot update baggage for completed check-in) |
| **404 Not Found** | Resource not found | Check-in, seat, booking, payment, or baggage reference does not exist |
| **409 Conflict** | Conflict with current state | Seat already held by another passenger; seat hold expired (confirm after TTL); duplicate or conflicting operation |
| **429 Too Many Requests** | Rate limit exceeded | Seat Management: more than 50 requests per 2 seconds per IP (see Rate Limiting) |
| **503 Service Unavailable** | Downstream service unavailable | Check-in when Seat/Baggage/Payment service is unreachable (e.g. circuit breaker open) |

Error response bodies typically include `message`, and optionally `timestamp`, `status`, and `errors` (for validation details).

---

## Common Response Formats

- **ApiResponse wrapper** (Baggage, Payment): `{ "success": boolean, "data": T, "message": string }`
- **Paginated** (Notifications): `{ "notifications": [], "currentPage": int, "totalItems": long, "totalPages": int }`
- **204 No Content**: Used for successful delete/cancel operations

---

## Service Ports (Development)

| Service | Port |
|---------|------|
| Seat Management | 8091 |
| Check-in | 8082 |
| Baggage | 8083 |
| Payment | 8084 |
| Notification | 8085 |