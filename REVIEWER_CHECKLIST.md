# Reviewer Feedback – Implementation Checklist

This document maps reviewer feedback to implementation details so you can verify all four points are covered.

---

## 1. API Error Handling, HTTP Status Codes, and Request Validations

### Implemented

| Area | Implementation | Location |
|------|----------------|----------|
| **HTTP status codes** | 400, 404, 409, 429, 503 used per API spec | All services; see API-SPECIFICATION.md "Error Responses" |
| **Validation (request body)** | `@Valid` on all DTOs; `MethodArgumentNotValidException` → 400 with `timestamp`, `status`, `error`, `message`, `errors` | SeatController, WaitlistController, CheckInController, BaggageController, PaymentController; GlobalExceptionHandler(s) |
| **Cancel – required passengerId** | `@RequestParam(required = true)` + explicit blank check; 400 if missing or blank | SeatController.cancelSeat(), Seat GlobalExceptionHandler |
| **Missing parameter** | `MissingServletRequestParameterException` → 400 | Seat GlobalExceptionHandler |
| **Type mismatch (e.g. invalid seatId)** | `MethodArgumentTypeMismatchException` → 400 | Seat GlobalExceptionHandler |
| **Illegal argument** | `IllegalArgumentException` → 400 | Seat GlobalExceptionHandler |
| **Business errors** | SeatNotFound → 404; SeatAlreadyHeld, SeatHoldExpired → 409; SeatUnavailable → 400; WaitlistNotFound → 404 | Seat/Check-in GlobalExceptionHandler |
| **Rate limit** | 429 with JSON body when limit exceeded | RateLimitFilter (Seat Management) |
| **Consistent error body** | `timestamp`, `status`, `error`, `message`, optional `errors` for validation | Seat & Check-in GlobalExceptionHandler |

### How to verify

- **Seat cancel without passengerId:** `DELETE /api/seats/1/cancel` → 400, body has `message` (e.g. required parameter 'passengerId' is missing).
- **Seat cancel with blank passengerId:** `DELETE /api/seats/1/cancel?passengerId= ` → 400.
- **Invalid path type:** `GET /api/seats/not-a-number/status` → 400.
- **Hold expired confirm:** Confirm after TTL → 409 Conflict, message "Seat hold has expired".
- **Validation:** POST with invalid/missing body fields → 400 with `errors` map.

---

## 2. Advanced Endpoints: Cancellation, Waitlist, Rate-Limiting

### Implemented

| Endpoint / feature | Description | Location |
|--------------------|-------------|----------|
| **Seat cancel** | `DELETE /api/seats/{seatId}/cancel?passengerId=...` – releases hold or cancels confirmed seat; passengerId required and validated | SeatController, SeatService |
| **Check-in cancel** | `POST /api/checkin/{checkinId}/cancel` – cancels in-progress check-in, releases seat hold | CheckInController, CheckInService |
| **Waitlist join** | `POST /api/seats/{seatId}/waitlist` – add passenger to waitlist | WaitlistController, WaitlistService |
| **Waitlist status** | `GET /api/seats/waitlist/{passengerId}` – all waitlist entries for passenger | WaitlistController |
| **Waitlist remove** | `DELETE /api/seats/waitlist/{waitlistId}` – remove entry; 404 if not found | WaitlistController, WaitlistService, WaitlistNotFoundException |
| **Rate-limiting** | 50 requests per 2 seconds per IP on `/api/seats/*`; 429 when exceeded; violations audited | RateLimitFilter, RateLimitAuditService, rate_limit_audit table |

### How to verify

- Cancel: successful cancel returns 204; wrong passenger → 400; missing passengerId → 400.
- Waitlist: join (201), get by passengerId (200), remove (204); remove with invalid id → 404.
- Rate limit: send >50 requests in 2s from same IP → 429 and JSON error body; check logs/audit for violation.

---

## 3. Test Coverage: Concurrency, Hold-Expiry, Edge Cases

### Implemented

| Scenario | Test type | Location |
|----------|-----------|----------|
| **Concurrency** | Two hold attempts for same seat; second gets conflict (lock not acquired) | SeatServiceTest.holdSeat_ConcurrentAttemptForSameSeat_SecondGetsConflict |
| **Hold expiry** | Confirm after expiry throws SeatHoldExpiredException | SeatServiceTest.confirmSeat_HoldExpired_ThrowsException |
| **Hold expiry API** | Confirm with expired hold returns 409 | SeatControllerTest.confirmSeat_HoldExpired_Returns409Conflict |
| **Expiry release** | SeatExpiryService releases expired holds, no-op when none | SeatExpiryServiceTest |
| **Cancel – missing/blank passengerId** | 400 for missing or blank passengerId | SeatControllerTest (cancelSeat_MissingPassengerId_BadRequest, cancelSeat_BlankPassengerId_BadRequest) |
| **Cancel – invalid seatId type** | 400 for non-numeric seatId | SeatControllerTest.cancelSeat_InvalidSeatIdType_BadRequest |
| **404 – seat not found** | getSeatStatus when seat missing → 404 | SeatControllerTest.getSeatStatus_SeatNotFound_Returns404 |
| **404 – waitlist not found** | removeFromWaitlist with invalid id → 404 | WaitlistControllerTest.removeFromWaitlist_NotFound_Returns404; WaitlistServiceTest |
| **Rate limiting** | Within limit allows through; over limit returns 429 and audits | RateLimitFilterTest |
| **Idempotent hold** | Same passenger re-hold returns existing hold | SeatServiceTest.holdSeat_shouldReturnSuccess_whenAlreadyHeldBySamePassenger |

### How to verify

- Run: `./mvnw test` (or per-service) in seat-management-service and checkin-service.
- Ensure SeatControllerTest and WaitlistControllerTest are enabled (no @Disabled).

---

## 4. Documentation: Security, Observability, Abuse Detection, Waitlist

### Implemented

| Topic | Where documented |
|-------|------------------|
| **Security** | ARCHITECTURE.md §12 (Threat model, auth/authz, RBAC plan, data protection). API-SPECIFICATION.md error codes and validation. |
| **Observability** | ARCHITECTURE.md §14 (Logging, metrics, health, audit tables, tracing). |
| **Abuse detection** | ARCHITECTURE.md §11 (Rate limiting, 429, audit of rate-limit events via rate_limit_audit). |
| **Waitlist** | ARCHITECTURE.md §4.2.1 and §7.3 (APIs, FIFO, assignment on release/expiry); API-SPECIFICATION.md §1.4 (endpoints and behaviour). |

### Expansions in ARCHITECTURE.md

- **§11 Rate Limiting & Abuse Detection:** Config (50/2s per IP), filter behaviour, 429 response, and that rate-limit violations are recorded for abuse analysis (rate_limit_audit); note that blocking (e.g. 5-minute block) is out of scope.
- **§12 Security:** Current scope (no auth, rate limiting only), threats not mitigated, and recommended production measures (JWT, RBAC, HTTPS, audit, input sanitization).
- **§14 Monitoring & Observability:** Log levels, key business and technical metrics, health endpoints, audit tables (seat_history, rate_limit_audit), and optional distributed tracing.
- **Waitlist:** Join, get by passenger, remove; FIFO; automatic assignment when seat released or hold expired; WaitlistProcessorScheduler.

### How to verify

- Open ARCHITECTURE.md and confirm sections 11, 12, 14 and waitlist references above.
- Open API-SPECIFICATION.md and confirm Error Responses table and Waitlist (§1.4) and validation rules.

---

## Quick Verification Commands

```bash
# Run all tests (from repo root)
./mvnw test

# Seat Management tests only
cd seat-management-service && ../mvnw test

# Check API spec and architecture docs
cat API-SPECIFICATION.md ARCHITECTURE.md | grep -E "429|409|404|400|Rate Limiting|Waitlist|Security|Observability"
```

---

**Document version:** 1.0  
**Last updated:** March 2026
