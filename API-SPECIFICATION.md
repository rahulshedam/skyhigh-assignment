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
| Notification | `/api/notifications`, `/api/otp` | Notification management and OTP services |
| Authentication | `/api/auth` (Seat Svc) | Email validation for login |

---

## 1. Authentication and Security

### 1.1 Header Authentication

Most services require the `X-User-Email` header for authenticated requests. This header should contain the logged-in passenger's email address.

| Header | Description | Required |
|--------|-------------|----------|
| `X-User-Email` | The email address of the authenticated passenger. | Yes (for protected endpoints) |

### 1.2 Authentication Flow

1. **Email Validation**: `POST /api/auth/validate-email` (Seat Management Service)
2. **OTP Generation**: `POST /api/otp/send` (Notification Service)
3. **OTP Verification**: `POST /api/otp/verify` (Notification Service)

---

## 1. Seat Management Service

### 1.1 Booking Verification

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/bookings/verify` | Verify booking reference and passenger ID; returns flight details | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found |

**Request body:** `BookingVerificationRequest` (bookingReference, passengerId)

---

### 1.2 Seat Map

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `GET` | `/api/seats/flights/{flightId}/seatmap` | Get complete seat map for a flight with availability status | **200** OK<br>**401** Unauthorized<br>**404** Not Found |

---

### 1.3 Seat Operations

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/seats/{seatId}/hold` | Hold a seat for a passenger (120-second expiry) | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**409** Conflict<br>**429** Too Many Requests |
| `POST` | `/api/seats/{seatId}/confirm` | Confirm a held seat assignment | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**409** Conflict |
| `DELETE` | `/api/seats/{seatId}/cancel` | Cancel a confirmed seat assignment | **204** No Content<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found |
| `GET` | `/api/seats/{seatId}/status` | Get current status of a seat | **200** OK<br>**401** Unauthorized<br>**404** Not Found |

**Cancel query param:** `passengerId` (required for cancel). Cancels a confirmed seat or releases a hold; after release, the waitlist processor may assign the seat to the next waitlisted passenger (FIFO).

---

### 1.4 Waitlist

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/seats/{seatId}/waitlist` | Add passenger to waitlist for a seat | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**409** Conflict |
| `GET` | `/api/seats/waitlist/{passengerId}` | Get all waitlist entries for a passenger | **200** OK<br>**401** Unauthorized |
| `DELETE` | `/api/seats/waitlist/{waitlistId}` | Remove passenger from waitlist | **204** No Content<br>**401** Unauthorized<br>**404** Not Found |

---

## 2. Baggage Service

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/baggage/validate` | Validate baggage weight and calculate excess fees | **200** OK<br>**400** Bad Request<br>**401** Unauthorized |
| `GET` | `/api/baggage/{baggageReference}` | Get baggage details by reference | **200** OK<br>**401** Unauthorized<br>**404** Not Found |
| `GET` | `/api/baggage/passenger/{passengerId}` | Get all baggage for a passenger | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/baggage/booking/{bookingReference}` | Get all baggage for a booking | **200** OK<br>**401** Unauthorized |

---

## 3. Check-in Service

The check-in flow supports an **explicit pause for payment** when excess baggage is required (weight > 25 kg). In that case the status becomes `WAITING_FOR_PAYMENT`; the client processes payment via the Payment Service and then **resumes** by calling the complete endpoint with the payment reference. Seat holds are subject to **Guaranteed Automatic Release** as detailed in ARCHITECTURE.md.

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/checkin/start` | Start check-in (seat hold + baggage validation) | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**409** Conflict<br>**503** Service Unavailable |
| `GET` | `/api/checkin/{checkinId}` | Get check-in status | **200** OK<br>**401** Unauthorized<br>**404** Not Found |
| `POST` | `/api/checkin/{checkinId}/complete` | Complete check-in (after payment if required); send `paymentId` when resuming from WAITING_FOR_PAYMENT | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**409** Conflict<br>**503** Service Unavailable |
| `POST` | `/api/checkin/{checkinId}/baggage` | Update baggage weight for in-progress check-in | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found |
| `POST` | `/api/checkin/{checkinId}/cancel` | Cancel an in-progress check-in (releases seat hold via Seat Management; idempotent if already cancelled) | **204** No Content<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found |

---

## Validation and Business Rules

- **Check-in:** `bookingReference`, `passengerId`, `flightId`, `seatId` required; `baggageWeight` optional, must be ≥ 0 if present. Excess baggage fee applies when weight > 25 kg.
- **Seat:** For hold and confirm: `passengerId`, `bookingReference` required. For cancel: `passengerId` query param required.
- **Baggage:** `passengerId`, `bookingReference`, `weight` (positive) required. Excess fee when weight > 25 kg (e.g. (weight − 25) × 10).
- **Payment:** Required fields depend on service (e.g. amount, passenger/booking reference); see Payment Service API.

---

## 4. Payment Service

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/payments/process` | Process a payment (excess baggage, etc.) | **200** OK<br>**400** Bad Request<br>**401** Unauthorized<br>**404** Not Found<br>**500** Internal Server Error |
| `GET` | `/api/payments/{paymentReference}` | Get payment details by reference | **200** OK<br>**401** Unauthorized<br>**404** Not Found |
| `GET` | `/api/payments/passenger/{passengerId}` | Get all payments for a passenger | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/payments/booking/{bookingReference}` | Get all payments for a booking | **200** OK<br>**401** Unauthorized |

---

## 5. Notification Service

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `GET` | `/api/notifications/hello` | Health check | **200** OK |
| `GET` | `/api/notifications` | Get all notifications (paginated) | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/notifications/{id}` | Get notification by ID | **200** OK<br>**401** Unauthorized<br>**404** Not Found |
| `GET` | `/api/notifications/recipient/{recipientId}` | Get notifications by recipient | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/notifications/status/{status}` | Get notifications by status | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/notifications/booking/{bookingReference}` | Get notifications by booking reference | **200** OK<br>**401** Unauthorized |
| `GET` | `/api/notifications/stats` | Get notification statistics | **200** OK<br>**401** Unauthorized |
| `POST` | `/api/notifications/test` | Send a test notification (demo/testing) | **200** OK<br>**401** Unauthorized |

**Notification list query params:** `page` (default 0), `size` (default 10), `sortBy` (default createdAt), `sortDir` (default DESC)

---

## 6. OTP Service (Notification Service)

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/otp/send` | Generate and send a 6-digit OTP to the provided email | **200** OK<br>**400** Bad Request |
| `POST` | `/api/otp/verify` | Verify the OTP for a given email (default: `123123`) | **200** OK<br>**400** Bad Request<br>**401** Unauthorized |

**OTP Send Request body:** `{ "email": "string" }`
**OTP Verify Request body:** `{ "email": "string", "otp": "string" }`

---

## 7. Email Validation (Seat Management Service)

| Method | Endpoint | Description | HTTP Status Codes |
|--------|----------|-------------|-------------------|
| `POST` | `/api/auth/validate-email` | Validate if an email exists as a passenger in the system | **200** OK<br>**400** Bad Request<br>**404** Not Found |

**Request body:** `{ "email": "string" }`
**Response body:** `{ "valid": boolean, "passengerName": "string", "message": "string" }`

---

## Standard Error Handling and HTTP Status Codes

All microservices adhere to standardized error responses and utilize semantic HTTP status codes.

### Standard HTTP Status Codes

| Status Code | Reason Phrase | Meaning and Usage |
|-------------|---------------|-------------------|
| **200** | OK | Request was successful. A response body is returned containing the requested data. |
| **201** | Created | A new resource was successfully created (e.g., establishing a seat hold). |
| **204** | No Content | Request was successful, but there is no data to return (e.g., canceling a seat, removing from waitlist). |
| **400** | Bad Request | The server could not understand the request due to invalid syntax, missing required parameters, or failed domain validation (e.g., negative baggage weight, missing `passengerId`). |
| **401** | Unauthorized | Authentication is required and has failed or has not yet been provided. Commonly returned when the `X-User-Email` header is missing or an invalid OTP is provided. |
| **403** | Forbidden | The user is authenticated but does not have permission to access the requested resource. |
| **404** | Not Found | The server cannot find the requested resource (e.g., check-in ID, seat ID, booking reference, or flight ID does not exist). |
| **409** | Conflict | The request conflicts with the current state of the server. Example scenarios: a seat is already held by another passenger, completing a check-in that is already complete, or hold TTL has expired. |
| **422** | Unprocessable Entity | The request was well-formed but was unable to be followed due to semantic errors (often used interchangeably with 400 for business logic validation failures). |
| **429** | Too Many Requests | The client has sent too many requests in a given amount of time. Applicable predominantly to the Seat Management service (e.g., > 50 requests per 2 seconds). |
| **500** | Internal Server Error | An unexpected condition was encountered on the server. Generic fallback for unhandled exceptions. |
| **503** | Service Unavailable | The server is currently unable to handle the request due to temporary overloading. Commonly encountered in the Check-in orchestration service when dependent downstream services (Seat, Baggage, Payment) are unreachable (e.g., Circuit Breaker is open). |

---

### Standardized Error Payload Definitions

Errors across the platform are returned using a consistent JSON structure to allow ease of processing on the client (frontend). 

#### 1. General Spring Boot / Basic Error Format
Primarily used for validation failures, routing issues, and fallback unhandled exceptions handled globally by Spring's `@ControllerAdvice`.

```json
{
  "timestamp": "2026-03-15T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Seat cannot be held. Maximum allowed limit reached.",
  "path": "/api/seats/12A/hold"
}
```

#### 2. Rate Limit Error Format (429)
The Seat Management Service returns a concise error payload when the rate limit (50 requests / 2s) is exceeded.

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

#### 3. Standard API Response Wrapper
Certain services (like Baggage and Payment), leveraging the `common` module, return a consistent `ApiResponse` wrapper payload that strictly encapsulates errors within the `error` object.

```json
{
  "success": false,
  "error": {
    "code": "SEAT_UNAVAILABLE",
    "message": "This seat is currently held by another passenger",
    "details": [
      "Wait until hold expires at 12:05 PM"
    ]
  },
  "timestamp": "2026-03-15T12:00:00.000Z"
}
```

**Common Error Codes:**
- `VALIDATION_FAILED`: Request payload strictly violates schema constraints.
- `RESOURCE_NOT_FOUND`: Referenced entity does not exist.
- `SEAT_UNAVAILABLE`: Conflict on specific seat layout operations.
- `SERVICE_UNAVAILABLE`: Upstream service timeout / circuit breaker trigger.
- `BAGGAGE_LIMIT_EXCEEDED`: Baggage rules violated.
- `PAYMENT_FAILED`: Payment processing gateway failure.

---

## Common Response Formats

- **ApiResponse wrapper** (Baggage, Payment): `{ "success": boolean, "data": T, "message": string }`
- **Paginated** (Notifications): `{ "notifications": [], "currentPage": int, "totalItems": long, "totalPages": int }`
- **204 No Content**: Used for successful delete/cancel operations (response body is empty).

---

## Service Ports (Development)

| Service | Port |
|---------|------|
| Seat Management | 8091 |
| Check-in | 8082 |
| Baggage | 8083 |
| Payment | 8084 |
| Notification | 8085 |