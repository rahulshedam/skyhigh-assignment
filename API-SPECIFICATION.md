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

**Cancel query param:** `passengerId` (required for cancel)

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

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/checkin/start` | Start check-in (seat hold + baggage validation) |
| `GET` | `/api/checkin/{checkinId}` | Get check-in status |
| `POST` | `/api/checkin/{checkinId}/complete` | Complete check-in (after payment if required) |
| `POST` | `/api/checkin/{checkinId}/baggage` | Update baggage weight for in-progress check-in |
| `POST` | `/api/checkin/{checkinId}/cancel` | Cancel an in-progress check-in |

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